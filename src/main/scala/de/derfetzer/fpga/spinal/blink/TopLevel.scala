package de.derfetzer.fpga.spinal.blink

import de.derfetzer.fpga.spinal.blackbox.IcebreakerPll
import de.derfetzer.fpga.spinal.pmod.{DA2, DA2FunctionGenerator}
import spinal.core._
import spinal.lib.Counter

class TopLevel extends Component {
  val io = new Bundle {
    val CLK = in Bool
    val LEDR_N = out Bool

    val BTN1 = in Bool
    val BTN2 = in Bool
    val BTN3 = in Bool

    val P1A7 = out Bool
    val P1A8 = out Bool
    val P1A9 = out Bool
    val P1A10 = out Bool
  }

  val pll = IcebreakerPll()
  pll.clock_in := io.CLK

  // Create own clock-domain for the
  // 60 Mhz Clock from PLL
  val extClockDomain = ClockDomain(
    clock = pll.clock_out,
    frequency = FixedFrequency(60 MHz),
    config = ClockDomainConfig(
      clockEdge = RISING,
      resetKind = BOOT,
      resetActiveLevel = LOW
    )
  )

  val area = new ClockingArea(extClockDomain) {
    // Blink
    val blink = new Blink

    io.LEDR_N := blink.io.led

    val da2fg = new DA2FunctionGenerator

    da2fg.io.stepSize := 5
    da2fg.io.stepSize(3) := io.BTN1
    da2fg.io.stepSize(4) := io.BTN2
    da2fg.io.stepSize(5) := io.BTN3

    io.P1A7 := da2fg.io.sync
    io.P1A8 := da2fg.io.dina
    io.P1A9 := da2fg.io.dinb
    io.P1A10 := da2fg.io.sclk
  }
}

//Generate the MyTopLevel's Verilog
object TopLevel {
  def main(args: Array[String]) {
    SpinalVerilog(new TopLevel)
  }
}