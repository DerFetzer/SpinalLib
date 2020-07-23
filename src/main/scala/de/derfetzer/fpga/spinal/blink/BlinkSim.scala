package de.derfetzer.fpga.spinal.blink

import spinal.core._
import spinal.sim._
import spinal.core.sim._

object BlinkSim {
  def main(args: Array[String]) {
    SimConfig.withWave.doSim(new Blink) {dut =>
      //Fork a process to generate the reset and the clock on the dut
      dut.clockDomain.forkStimulus(period = 10)

      while(true) {
        dut.clockDomain.waitRisingEdge()

        if (dut.counter.toLong == math.pow(2, 20)) {
          if (dut.io.led.toBoolean) {
            print(dut.counter.toLong)
            simSuccess()
          }
          else {
            simFailure("LED is not activated")
          }
        }
        else {
          assert(!dut.io.led.toBoolean)
        }
      }
    }
  }
}
