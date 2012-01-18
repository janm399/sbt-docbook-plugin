sbtPlugin := true

name := "sbt-docbook-plugin"

organization := "de.undercouch"

version := "0.2-SNAPSHOT"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
	"org.apache.xmlgraphics" % "fop" % "1.0",
	"xerces" % "xercesImpl" % "2.10.0"
	)

seq(scriptedSettings: _*)

publishTo <<= (version) { version: String =>
  val nexus = "http://nexus.scala-tools.org/content/repositories/"
  if (version.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "snapshots/")
  else Some("releases" at nexus + "releases/")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
