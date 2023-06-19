package core

import chisel3._
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}
import chisel3.util.log2Ceil
import chisel3.util.MixedVec


object main extends App {

        val numberOfOutputQueues = 8

        //Vec 0 is "hit identifier" feature based on the exemplary detector description. We ignore the rest
        def onlineEdgeCondition(a :MixedVec[UInt], b :MixedVec[UInt]): Bool = {
            a(0) === true.B && b(0) === true.B
        } 

        /* Load IntermediateAlgroithmRepresentation from JSON File in the Resource Folder */
        val iarResource = scala.io.Source.fromResource("igr.json")
        val iarString = iarResource.getLines().mkString
        iarResource.close()

        val graphBuildingContext = new graph.GraphBuildingContext(iarString, numberOfOutputQueues, onlineEdgeCondition)

        (new ChiselStage).execute(
            Array("--target", "systemverilog","--target-dir", "rtl"),
            Seq(ChiselGeneratorAnnotation(() => new graph.GraphBuildingElement(graphBuildingContext)),
            FirtoolOption("--disable-all-randomization"))
        )
}
