import scala.sys.process._

name := "SpinalLib"
version := "1.0"
scalaVersion := "2.11.12"
val spinalVersion = "1.4.0"

libraryDependencies ++= Seq(
  "com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion,
  "com.github.spinalhdl" %% "spinalhdl-lib" % spinalVersion,
  compilerPlugin("com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion),

  "org.scalatest" %% "scalatest-funsuite" % "3.2.0" % "test",
  "org.scalatest" %% "scalatest-mustmatchers" % "3.2.0" % "test",
)

fork := true
EclipseKeys.withSource := true

lazy val synth = taskKey[Unit]("Synthesize bitstream")
lazy val upload = taskKey[Unit]("Upload bitstream to board")

synth := {
  val apioBuild = Seq("apio", "build")

  (runMain in Compile).toTask(" de.derfetzer.fpga.spinal.blink.TopLevel").value

  apioBuild !
}

upload := {
  synth.value

  val apioUpload = Seq("apio","upload")

  apioUpload !
}

cleanFiles += baseDirectory.value / "TopLevel.v"
cleanFiles += baseDirectory.value / "hardware.asc"
cleanFiles += baseDirectory.value / "hardware.bin"
cleanFiles += baseDirectory.value / "hardware.json"
