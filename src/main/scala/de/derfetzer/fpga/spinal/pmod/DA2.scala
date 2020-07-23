package de.derfetzer.fpga.spinal.pmod

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.fsm._
import spinal.lib.com.spi.ddr.{SpiXdrMasterCtrl, SpiXdrParameter}

class DA2 extends Component{
  val io = new Bundle {
    val active = in Bool
    val write = in Bool

    val data1 = slave Stream Bits(16 bits)
    val data2 = slave Stream Bits(16 bits)

    val busy = out Bool

    val sync = out Bool
    val dina = out Bool
    val dinb = out Bool
    val sclk = out Bool
  }

  val spiParam = SpiXdrMasterCtrl.Parameters(16, 4, SpiXdrParameter(dataWidth = 2, ssWidth = 1, ioRate = 1)).addFullDuplex(0)
  val spiConfig = SpiXdrMasterCtrl.Config(spiParam)

  spiConfig.sclkToogle := 0
  spiConfig.kind.cpol := False
  spiConfig.kind.cpha := True
  spiConfig.ss.activeHigh := 0
  spiConfig.ss.disable := 0
  spiConfig.ss.hold := 1
  spiConfig.ss.setup := 1

  var spi1 = SpiXdrMasterCtrl(spiParam)
  var spi2 = SpiXdrMasterCtrl(spiParam)

  io.sync := spi1.io.spi.ss(0)
  io.sclk := spi1.io.spi.sclk.write(0)
  io.dina := spi1.io.spi.data(0).write(0)
  io.dinb := spi2.io.spi.data(0).write(0)

  spi1.io.config := spiConfig
  spi2.io.config := spiConfig

  val sendFsm = new StateMachine {
    val stateIdle = new State with EntryPoint
    val stateSSActivate = new State
    val stateSSDeactivate = new State
    val stateData = new State

    val data1Reg = Reg(Bits(16 bits))
    val data2Reg = Reg(Bits(16 bits))

    spi1.io.cmd.payload.kind := False
    spi1.io.cmd.payload.write := True
    spi1.io.cmd.payload.data.setAll()
    spi1.io.cmd.valid := False

    spi2.io.cmd.payload.kind := False
    spi2.io.cmd.payload.write := True
    spi2.io.cmd.payload.data.setAll()
    spi2.io.cmd.valid := False

    io.data1.ready := False
    io.data2.ready := False
    io.busy := False

    stateIdle.whenIsActive {
      io.busy := False
      spi1.io.cmd.valid := False
      spi2.io.cmd.valid := False

      when(io.active && io.data1.valid && io.data2.valid && io.active && io.write.rise()) {
        data1Reg := io.data1.payload
        data2Reg := io.data2.payload

        goto(stateSSActivate)
      }
    }

    stateSSActivate.whenIsActive {
      io.busy := True

      spi1.io.cmd.payload.kind := True
      spi1.io.cmd.payload.data.msb := True
      spi1.io.cmd.payload.data.lsb := True

      spi1.io.cmd.valid := True

      spi2.io.cmd.payload.kind := True
      spi2.io.cmd.payload.data.msb := True
      spi2.io.cmd.payload.data.lsb := True

      spi2.io.cmd.valid := True

      when(spi1.io.cmd.ready) {
        goto(stateData)
      }
    }

    stateSSDeactivate.whenIsActive {
      io.busy := True

      spi1.io.cmd.payload.kind := True
      spi1.io.cmd.payload.data.assignFromBits(0)

      spi1.io.cmd.valid := True

      spi2.io.cmd.payload.kind := True
      spi2.io.cmd.payload.data.assignFromBits(0)

      spi2.io.cmd.valid := True

      when(spi1.io.cmd.ready) {
        goto(stateIdle)
      }

    }

    stateData.whenIsActive {
      io.busy := True

      spi1.io.cmd.payload.kind := False
      spi1.io.cmd.payload.data := data1Reg

      spi1.io.cmd.valid := True

      spi2.io.cmd.payload.kind := False
      spi2.io.cmd.payload.data := data2Reg

      spi2.io.cmd.valid := True

      io.data1.ready := True
      io.data2.ready := True

      when(spi1.io.cmd.ready) {
        goto(stateSSDeactivate)
      }
    }
  }
}
