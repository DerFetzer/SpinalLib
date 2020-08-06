package de.derfetzer.fpga.spinal.pmod

import spinal.core._
import spinal.lib.Counter

case class DA2FunctionGeneratorGenerics(resolutionWidth: Int = 12, sampleCount: Int = 2048, fractionBits: Int = 6)

class DA2FunctionGenerator(g: DA2FunctionGeneratorGenerics) extends Component {
  val io = new Bundle {
    val enable = in Bool

    val prescaler = in UInt(32 bits)
    val fcw = in UInt (log2Up(g.sampleCount) + g.fractionBits bits)

    val lutEnable = in Bool
    val lutWrite = in Bool
    val lutAddress = in UInt(log2Up(g.sampleCount) bits)
    val lutWriteData = in UInt(g.resolutionWidth bits)
    val lutReadData = out UInt(g.resolutionWidth bits)

    val sync = out Bool
    val dina = out Bool
    val dinb = out Bool
    val sclk = out Bool
  }

  ClockDomain.current match {
    case u if u.frequency == UnknownFrequency() => ()
    case c => assert(c.frequency.getMax.toInt <= 60e9)
  }

  def sinTable = for (sampleIndex <- 0 until g.sampleCount) yield {
    val sinValue = Math.sin(2 * Math.PI * sampleIndex / g.sampleCount) + 1
    U((sinValue * ((1 << g.resolutionWidth) / 2 - 1)).toInt, g.resolutionWidth bits)
  }

  val da2 = new DA2

  val cnt = Counter(32 bits)
  when (io.enable) {
    cnt.increment()
  }.otherwise {
    cnt.clear()
    cnt.willIncrement := False
  }

  val rom = Mem(UInt(g.resolutionWidth bits), initialContent = sinTable)
  io.lutReadData := rom.readWriteSync(
    enable  = io.lutEnable,
    address = io.lutAddress,
    data    = io.lutWriteData,
    write   = io.lutWrite
  )

  val phase = Reg(UInt(log2Up(g.sampleCount) + g.fractionBits bits)) init (0)
  when(cnt.value === io.prescaler - 1) {
    cnt.clear()
    phase := phase + io.fcw
    da2.io.write := True
  }.otherwise {
    da2.io.write := False
  }

  io.sync := da2.io.sync
  io.dina := da2.io.dina
  io.dinb := da2.io.dinb
  io.sclk := da2.io.sclk

  da2.io.active := io.enable
  da2.io.data1.payload(15 downto 12).setAllTo(False)
  da2.io.data1.payload(11 downto 0) := rom.readSync(phase.floor(g.fractionBits)).asBits
  da2.io.data1.valid := True

  da2.io.data2.payload.setAllTo(False)
  da2.io.data2.valid := True
}

object DA2FunctionGenerator {
  def main(args: Array[String]) {
    SpinalVerilog(new DA2FunctionGenerator(DA2FunctionGeneratorGenerics()))
  }
}