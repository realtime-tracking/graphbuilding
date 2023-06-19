package graph

import chisel3._
import chisel3.util.MixedVec
import chisel3.util.Counter
import chisel3.util.ShiftRegister
import chisel3.util.log2Ceil

import chisel3.experimental.hierarchy.{Definition, instantiable, public}


@instantiable
class EdgeProcessingElement(context: EdgeProcessingElementContext) extends Module {

    @public val io = IO( new Bundle {
        val verticesA = Input(Vec(context.tMux,context.sensorIO))
        val verticesB = Input(Vec(context.tMux,context.sensorIO))
        val valid = Input(Bool())
        val edges = Output(Vec(context.tMux,context.edgeIO))
        val ready = Output(Bool())
    })

    val latency = 2
    
    val readyReg = RegInit(true.B)
    io.ready := readyReg

    val cIn = Counter(context.tMux)
    val cOut = ShiftRegister(cIn.value,latency)

    val verticesAReg = Reg(Vec(context.tMux,context.sensorIO))
    val verticesBReg = Reg(Vec(context.tMux,context.sensorIO))

    val vertexAWire = Wire(context.sensorIO)
    val vertexBWire = Wire(context.sensorIO)

    val verticesARomList = for((featureSeq,featureWidth) <- (context.verticesAStaticFeatures zip context.vertexStaticFeaturesWidth))
        yield VecInit(featureSeq.map(x => x.U(featureWidth.W)))

    val verticesBRomList = for((featureSeq,featureWidth) <- (context.verticesBStaticFeatures zip context.vertexStaticFeaturesWidth))
        yield VecInit(featureSeq.map(x => x.U(featureWidth.W)))

    val edgeRomList = for((featureSeq,featureWidth) <- (context.edgeStaticFeatures zip context.edgeStaticFeaturesWidth))
        yield VecInit(featureSeq.map(x => x.U(featureWidth.W)))

    val vertexARomWire = for (featureWidth <- context.vertexStaticFeaturesWidth)
        yield Wire(Vec(1,UInt(featureWidth.W)))

    val vertexBRomWire = for (featureWidth <- context.vertexStaticFeaturesWidth)
        yield Wire(Vec(1,UInt(featureWidth.W)))

    val edgeRomWire = for (featureWidth <- context.edgeStaticFeaturesWidth)
        yield Wire(Vec(1,UInt(featureWidth.W)))

    when(io.valid === true.B && readyReg === true.B) {
        verticesAReg := io.verticesA
        verticesBReg := io.verticesB
        cIn.reset()
        readyReg := false.B
    } .otherwise {
        when(cIn.value < (context.tMux - 1).U) {
            cIn.inc()
        } .otherwise {
            when(cOut === (context.tMux - 1).U) {
                readyReg := true.B
            }
        }
    }

    vertexAWire := verticesAReg(cIn.value)
    vertexBWire := verticesBReg(cIn.value)

    for(i <- 0 until context.vertexStaticFeaturesNum) {
        vertexARomWire(i)(0) := verticesARomList(i)(cIn.value)
        vertexBRomWire(i)(0) := verticesBRomList(i)(cIn.value)
    }

    for(i <- 0 until context.edgeStaticFeaturesNum) {
        edgeRomWire(i)(0) := edgeRomList(i)(cIn.value)
    }
    
    val edgeReg = RegInit(0.U.asTypeOf(context.edgeIO))

    val edgeWire = Wire(context.edgeIO)
    edgeWire.active := context.edgeCondition(vertexAWire.features,vertexBWire.features)
    edgeWire.features := ((vertexAWire.features.toIndexedSeq ++
                               vertexARomWire.map(_.toIndexedSeq).reduce(_ ++ _) ++
                               vertexBWire.features.toIndexedSeq ++
                               vertexBRomWire.map(_.toIndexedSeq).reduce(_ ++ _) ++ 
                               edgeRomWire.map(_.toIndexedSeq).reduce(_ ++ _)))
    edgeReg := RegNext(edgeWire)
    
    val edgesReg = RegInit(VecInit(Seq.fill(context.tMux)(0.U.asTypeOf(context.edgeIO))))
    edgesReg(cOut) := edgeReg
    io.edges := edgesReg



}