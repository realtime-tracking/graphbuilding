package graph

import chisel3._
import chisel3.util.MixedVec
import chisel3.util.Counter
import chisel3.util.ShiftRegister
import chisel3.util.log2Ceil

import chisel3.experimental.hierarchy.{Definition, Instance, instantiable, public}


@instantiable
class GraphBuildingElement(context: GraphBuildingContext) extends Module {

    @public val io = IO( new Bundle {
        val detector = Input(context.detector.detectorIO)
        val graph = Output(context.graph.graphIO)
    })

    val edgePeList = for (i <- 0 until context.ePeNum) yield {
        val edgePe = Instance(Definition(new EdgeProcessingElement(context.ePeContext(i))))

        edgePe.io.verticesA := 0.U.asTypeOf(chiselTypeOf(edgePe.io.verticesA))
        edgePe.io.verticesB := 0.U.asTypeOf(chiselTypeOf(edgePe.io.verticesB))

        edgePe.io.valid := io.detector.valid

        edgePe.io.valid := io.detector.valid

         context.ePeVerticesA(i).zipWithIndex.foreach {
            case(id, j) =>
                edgePe.io.verticesA(j) := io.detector.sensors(id)
         }

        context.ePeVerticesB(i).zipWithIndex.foreach {
            case(id, j) =>
                edgePe.io.verticesB(j) := io.detector.sensors(id)
         }

        context.ePeEdgeIds(i).zipWithIndex.foreach {
            case(id, j) =>
                io.graph.edges(id) := edgePe.io.edges(j)
        }
        edgePe
    }
    io.graph.valid := VecInit(edgePeList.map(_.io.ready)).reduceTree(_ && _)
}