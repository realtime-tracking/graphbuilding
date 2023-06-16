package graph

import scala.language.postfixOps

import chisel3._
import chisel3.util.MixedVec

import io.circe._, io.circe.parser._, io.circe.generic.semiauto._
import IarDecoder._

case class DetectorContext(val sensorNum: Int,
                          val sensorDynamicFeaturesName: Seq[String],
                          val sensorDynamicFeaturesWidth: Seq[Int],
                          val sensorStaticFeaturesName: Seq[String],
                          val sensorStaticFeaturesWidth: Seq[Int]) {

    val sensorDynamicFeaturesNum = sensorDynamicFeaturesName.length
    val sensorStaticFeaturesNum = sensorStaticFeaturesName.length

    val sensorIO = new SensorIO(sensorDynamicFeaturesWidth)
    val detectorIO = new DetectorIO(sensorNum,sensorIO)
}

case class GraphContext(edgeNum: Int,
                        vertexNum: Int,
                        vertexFeatures: Seq[String],
                        vertexFeaturesWidth: Seq[Int],
                        edgeStaticFeatures: Seq[String],
                        edgeStaticFeaturesWidth: Seq[Int]) {

    val vertexFeaturesNum = vertexFeatures.size

    val edgeStaticFeaturesNum = edgeStaticFeatures.size

    val edgeFeatures = vertexFeatures ++ vertexFeatures ++ edgeStaticFeatures
    val edgeFeaturesNum = edgeFeatures.size
    val edgeFeaturesWidth = vertexFeaturesWidth ++ vertexFeaturesWidth ++ edgeStaticFeaturesWidth

    val edgeIO = new EdgeIO(edgeFeaturesWidth)
    val graphIO = new GraphIO(edgeNum, edgeIO)
}

case class EdgeProcessingElementContext(sensorIO: SensorIO,
                                        edgeIO: EdgeIO,
                                        tMux: Int,
                                        edgeCondition: (MixedVec[UInt],MixedVec[UInt]) => Bool, 
                                        verticesAStaticFeatures: Seq[Seq[String]],
                                        verticesBStaticFeatures: Seq[Seq[String]],
                                        vertexStaticFeaturesWidth: Seq[Int],
                                        edgeStaticFeatures: Seq[Seq[String]],
                                        edgeStaticFeaturesWidth: Seq[Int]) {
    val vertexStaticFeaturesNum = vertexStaticFeaturesWidth.length
    val edgeStaticFeaturesNum = edgeStaticFeaturesWidth.length

}

class GraphBuildingContext(iarJson: String, val tMux: Int, edgeCondition: (MixedVec[UInt],MixedVec[UInt]) => Bool) {

    val iar = decode[IntermediateAlgroithmRepresentation](iarJson) match {
        case Left(error) => throw new Exception(error) 
        case Right(iar) => iar
    }

    val vertexNum :Int = iar.vertices.length
    val edgeNum :Int = iar.edges.length

    println(f"Found algorithmic graph description with $vertexNum vertices and $edgeNum edges.")

    val vertexStaticFeaturesName :Seq[String] = iar.vertexFeatures.filter(_.kind=="static").map(_.name)
    val vertexStaticFeaturesWidth :Seq[Int] = iar.vertexFeatures.filter(_.kind=="static").map(_.resolution)

    val vertexDynamicFeaturesName :Seq[String]  = iar.vertexFeatures.filter(_.kind=="dynamic").map(_.name)
    val vertexDynamicFeaturesWidth :Seq[Int] = iar.vertexFeatures.filter(_.kind=="dynamic").map(_.resolution)
    val vertexStaticFeatures :Seq[Seq[String]]  = iar.vertices.map(_.staticFeatures).toList.transpose
    val vertexStaticFeaturesMaps :Seq[Map[Int,String]] = vertexStaticFeatures.map(_.zipWithIndex.map(_.swap) toMap)

    val edgeStaticFeaturesName :Seq[String] = iar.edgeFeatures.filter(_.kind=="static").map(_.name)
    val edgeStaticFeaturesWidth :Seq[Int] = iar.edgeFeatures.filter(_.kind=="static").map(_.resolution)
    val edgesStaticFeatures :Seq[Seq[String]] = iar.edges.map(_.staticFeatures).toList.transpose
    val edgeStaticFeatureMap :Seq[Map[Int,String]] = edgesStaticFeatures.map(_.zipWithIndex.map(_.swap) toMap)
    
    val edgeIds :Seq[Int] = iar.edges.map(_.id).toList
    val edgeVerticesA :Seq[Int] = iar.edges.map(_.vertexA).toList
    val edgeVerticesB :Seq[Int] = iar.edges.map(_.vertexB).toList

    val detector = new DetectorContext(vertexNum,
                                       vertexDynamicFeaturesName,
                                       vertexDynamicFeaturesWidth,
                                       vertexStaticFeaturesName,
                                       vertexStaticFeaturesWidth)

    var graph = new GraphContext(edgeNum,
                                 vertexNum,
                                 vertexDynamicFeaturesName ++ vertexStaticFeaturesName,
                                 vertexDynamicFeaturesWidth ++ vertexStaticFeaturesWidth,
                                 edgeStaticFeaturesName,
                                 edgeStaticFeaturesWidth)

    val ePeNum :Int = (graph.edgeNum.toFloat / tMux.toFloat).ceil.toInt
    val ePeEdgeIds :Seq[Seq[Int]] = edgeIds.grouped(tMux).toSeq
    val ePeVerticesA :Seq[Seq[Int]] = edgeVerticesA.grouped(tMux).toSeq
    val ePeVerticesB :Seq[Seq[Int]] = edgeVerticesB.grouped(tMux).toSeq

    val ePeContext : Seq[EdgeProcessingElementContext] =
        for(i <- 0 until ePeNum) yield {

            val verticesAStaticFeatures = for (featureMap <- vertexStaticFeaturesMaps)
                yield ePeVerticesA(i).map(x => featureMap(x))

            val verticesBStaticFeatures = for (featureMap <- vertexStaticFeaturesMaps)
                yield ePeVerticesB(i).map(x => featureMap(x))

            val ePeEdgesStaticFeatures = for (featureMap <- edgeStaticFeatureMap)
                yield ePeEdgeIds(i).map(x => featureMap(x))

            new EdgeProcessingElementContext(detector.sensorIO,
                                             graph.edgeIO,
                                             tMux,
                                             edgeCondition,
                                             verticesAStaticFeatures,
                                             verticesBStaticFeatures,
                                             vertexStaticFeaturesWidth,
                                             ePeEdgesStaticFeatures,
                                             edgeStaticFeaturesWidth)
        }
}
