package de.derfetzer.fpga.spinal.pmod

import spinal.sim._
import spinal.core.sim._
import spinal.core._

object DA2Sim {
  def main(args: Array[String]) {
    SimConfig.withWave.doSim(new DA2) {dut =>
      dut.io.data1.payload #= 0xABAB
      dut.io.data2.payload #= 0xAAAA
      dut.io.data1.valid #= true
      dut.io.data2.valid #= true

      dut.io.active #= true
      dut.io.write #= false

      //Fork a process to generate the reset and the clock on the dut
      dut.clockDomain.forkStimulus(period = 10)

      dut.clockDomain.waitRisingEdge(20)

      dut.io.write #= true

      while(!dut.io.busy.toBoolean) {
        dut.clockDomain.waitRisingEdge()
      }

      dut.clockDomain.waitRisingEdge(10)

      for(idx <- 0 to 100) {
        dut.clockDomain.waitRisingEdge()

        if (!dut.io.busy.toBoolean) {
          dut.io.data1.valid #= false
          dut.io.active #= false
        }
      }
    }
  }
}
