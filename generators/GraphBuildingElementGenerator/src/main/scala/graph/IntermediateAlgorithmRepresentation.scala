
package graph

import io.circe._, io.circe.generic.semiauto._

case class Nested(arrayField: List[Int])


case class Feature(name: String, kind: String, resolution: Int, sign: Int, range: Seq[Int])
case class Vertex(id: Int, staticFeatures: Seq[String]) 
case class Edge(id: Int, staticFeatures: Seq[String], vertexA: Int, vertexB: Int) 

case class IntermediateAlgroithmRepresentation(
  vertexFeatures: Seq[Feature],
  edgeFeatures: Seq[Feature],
  vertices: Seq[Vertex],
  edges: Seq[Edge]
)

object IarDecoder {
    implicit val featureDecoder: Decoder[Feature] = deriveDecoder[Feature]
    implicit val vertexDecoder: Decoder[Vertex] = deriveDecoder[Vertex]
    implicit val edgesDecoder: Decoder[Edge] = deriveDecoder[Edge]
    implicit val iarDecoder: Decoder[IntermediateAlgroithmRepresentation] = deriveDecoder[IntermediateAlgroithmRepresentation]
}