package de.derfetzer.fpga.spinal.pmod

import spinal.core._
import spinal.lib.Counter

class DA2FunctionGenerator extends Component {
  val io = new Bundle {
    val stepSize = in UInt (8 bits)

    val sync = out Bool
    val dina = out Bool
    val dinb = out Bool
    val sclk = out Bool
  }

  assert(ClockDomain.current.frequency.getMax.toInt <= 60e9)

  val resolutionWidth = 12
  val sampleCount = 2048

  def sinTable = for (sampleIndex <- 0 until sampleCount) yield {
    val sinValue = Math.sin(2 * Math.PI * sampleIndex / sampleCount) + 1
    U((sinValue * ((1 << resolutionWidth) / 2 - 1)).toInt, resolutionWidth bits)
  }

  val da2 = new DA2

  val cnt = Counter(7 bits)
  cnt.increment()

  val rom = Mem(UInt(resolutionWidth bits), initialContent = sinTable)
  val phase = Reg(UInt(log2Up(sampleCount) bits)) init (0)
  when(cnt.willOverflow) {
    phase := phase + io.stepSize
    da2.io.write := True
  }.otherwise {
    da2.io.write := False
  }

  io.sync := da2.io.sync
  io.dina := da2.io.dina
  io.dinb := da2.io.dinb
  io.sclk := da2.io.sclk

  da2.io.active := True
  da2.io.data1.payload(15 downto 2).setAllTo(False)
  da2.io.data1.payload(11 downto 0) := rom.readSync(phase).asBits // cnt.value.asBits(19 downto 8)
  da2.io.data1.valid := True

  da2.io.data2.payload.setAllTo(False)
  da2.io.data2.valid := True
}
