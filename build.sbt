sbtPlugin := true

name := "sbt-docbook-plugin"

organization := "de.undercouch"

version := "0.2-SNAPSHOT"

libraryDependencies += "org.apache.xmlgraphics" % "fop" % "1.0"

seq(scriptedSettings: _*)
