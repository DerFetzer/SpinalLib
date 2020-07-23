package de.derfetzer.fpga.spinal.blink

import spinal.core._
import spinal.sim._
import spinal.core.sim._

//TopLevel's testbench
object TopLevelSim {
  def main(args: Array[String]) {
    SimConfig.withWave.doSim(new TopLevel) {dut =>
      //Fork a process to generate the reset and the clock on the dut
      val cd = ClockDomain(dut.io.CLK)
      cd.forkStimulus(period = 10)

      for(idx <- 0 to 1000){
        //Wait a rising edge on the clock
        cd.waitRisingEdge()
      }
    }
  }
}
