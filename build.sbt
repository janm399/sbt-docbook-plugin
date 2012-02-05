sbtPlugin := true

name := "sbt-docbook-plugin"

organization := "de.undercouch"

version := "0.2-SNAPSHOT"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
	"org.apache.xmlgraphics" % "fop" % "1.0",
	"xerces" % "xercesImpl" % "2.10.0",
	"saxon" % "saxon" % "6.5.3",
	"xml-resolver" % "xml-resolver" % "1.2",
	"net.sf.docbook" % "docbook-xsl" % "1.76.1",
	"net.sf.docbook" % "docbook-xsl-saxon" % "1.0.0",
	"net.sf.xslthl" % "xslthl" % "2.0.2"
	)

seq(scriptedSettings: _*)

publishTo <<= (version) { version: String =>
  val nexus = "http://nexus.scala-tools.org/content/repositories/"
  if (version.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "snapshots/")
  else Some("releases" at nexus + "releases/")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
