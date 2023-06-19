package graph

case class GraphConfig() {
    val edgeNum = 8

    val vertexNum = 8

    //Assign a unique Id per Edge(vertexA,vertexB)
    val edgeMap = Map(0 -> (0,1), 1 -> (1,4), 2 -> (1,2), 3 -> (3,4), 4 -> (4,7), 5 -> (2,6), 6 -> (7,0), 7 -> (2,5))
    //Assign a set of edges to a vertex 
    val vertexMap = Map(0 -> List(0,6), 1 -> List(0,1,2), 2 -> List(2,5,7), 3 -> List(3), 4 -> List(1,3,4), 5 -> List(7), 6 -> List(5), 7 -> List(4,6))

    val maxDegree = 3

}