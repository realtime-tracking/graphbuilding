package graph

import chisel3._
import chisel3.util.MixedVec

class EdgeIO(width: Seq[Int]) extends Bundle {
    val features = MixedVec(width.map(x => UInt(x.W)).toSeq)
    val active = Bool()
}

class GraphIO[T <: EdgeIO](edgeNum: Int, edgeIO: T) extends Bundle {
    val edges = Vec(edgeNum,edgeIO)
    val valid = Bool()
}

class SensorIO(width: Seq[Int]) extends Bundle {
    val features = MixedVec(width.map(x => UInt(x.W)).toSeq)
}

class DetectorIO[T <: SensorIO](sensorNum: Int, sensorIO: T) extends Bundle {
    val sensors = Vec(sensorNum,sensorIO)
    val valid = Bool()
}