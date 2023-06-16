package core

import org.scalatest._

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._
import chisel3.util.MixedVec



class GraphBuildingElementSpec extends AnyFlatSpec with ChiselScalatestTester {
    it should "detect all edges" in {

        val numberOfOutputQueues = 8

        //Vec 0 is "hit identifier" feature based on the exemplary detector description. We ignore the rest
        def onlineEdgeCondition(a :MixedVec[UInt], b :MixedVec[UInt]): Bool = {
            a(0) === true.B && b(0) === true.B
        } 

        /* Load IntermediateAlgroithmRepresentation from JSON File in the Resource Folder */
        val iarResource = scala.io.Source.fromResource("iar.json")
        val iarString = iarResource.getLines().mkString
        iarResource.close()

        val graphBuildingContext = new graph.GraphBuildingContext(iarString, numberOfOutputQueues,onlineEdgeCondition)

        test(new graph.GraphBuildingElement(graphBuildingContext)) { c =>

                c.reset.poke(true.B)
                c.clock.step()
                c.reset.poke(false.B)

                c.io.detector.sensors.zipWithIndex.foreach {
                    case(sensor,i) => {
                        sensor.features(0).poke("b1".U)
                        sensor.features(1).poke("b1011".U)
                        sensor.features(2).poke("b11111".U)
                    }
                }
                c.io.detector.valid.poke(true.B)
                c.clock.step()
                c.io.detector.valid.poke(false.B)
                c.clock.step()

                var count = 1
                while(c.io.graph.valid.peek().litValue == BigInt(0) && count < 20) {
                    c.clock.step()
                    count += 1
                    println(f"Cycle $count, ${c.io.graph.edges(0).peek()}.")
                }
                println(f"Latency of $count cycles until all edges have been checked.")

                c.io.graph.edges.zipWithIndex.foreach {
                    case(edge,i) => {
                        edge.active.expect(1.U)
                        edge.features(0).expect("b1".U)
                        edge.features(1).expect("b1011".U)
                        edge.features(2).expect("b11111".U)
                        edge.features(5).expect("b1".U)
                        edge.features(6).expect("b1011".U)
                        edge.features(7).expect("b11111".U)
                    }
                }
                c.clock.step()
        }
    }

    it should "detect no edges" in {

        val numberOfOutputQueues = 8

        //Vec 0 is "hit identifier" feature based on the exemplary detector description. We ignore the rest
        def onlineEdgeCondition(a :MixedVec[UInt], b :MixedVec[UInt]): Bool = {
            a(0) === true.B && b(0) === true.B
        } 

        /* Load IntermediateAlgroithmRepresentation from JSON File in the Resource Folder */
        val iarResource = scala.io.Source.fromResource("iar.json")
        val iarString = iarResource.getLines().mkString
        iarResource.close()

        val graphBuildingContext = new graph.GraphBuildingContext(iarString, numberOfOutputQueues,onlineEdgeCondition)

        test(new graph.GraphBuildingElement(graphBuildingContext)) { c =>

                c.reset.poke(true.B)
                c.clock.step()
                c.reset.poke(false.B)

                c.io.detector.sensors.zipWithIndex.foreach {
                    case(sensor,i) => {
                        sensor.features(0).poke("b0".U)
                        sensor.features(1).poke("b1111".U)
                        sensor.features(2).poke("b11111".U)
                    }
                }
                c.io.detector.valid.poke(true.B)
                c.clock.step()
                c.io.detector.valid.poke(false.B)


                var count = 0
                while(c.io.graph.valid.peek().litValue == BigInt(0) && count < 20) {
                    c.clock.step()
                    count += 1
                }
                println(f"Latency of $count cycles until all edges have been checked.")

                c.io.graph.edges.zipWithIndex.foreach {
                    case(edge,i) => {
                        edge.active.expect(0.U)
                        edge.features(0).expect("b0".U)
                        edge.features(1).expect("b1111".U)
                        edge.features(2).expect("b11111".U)
                        edge.features(5).expect("b0".U)
                        edge.features(6).expect("b1111".U)
                        edge.features(7).expect("b11111".U)
                    }
                }
                c.clock.step()
        }
    }
}
