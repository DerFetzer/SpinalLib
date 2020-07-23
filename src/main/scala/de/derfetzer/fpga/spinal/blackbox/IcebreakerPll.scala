package de.derfetzer.fpga.spinal.blackbox

import spinal.core._

case class IcebreakerPll() extends BlackBox {
  setDefinitionName("icebreaker_pll")
  val clock_in = in Bool()
  val clock_out = out Bool()
  val locked = out Bool()
}
