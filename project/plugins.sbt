resolvers += "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "0.11.0")

libraryDependencies <+= (sbtVersion) { (version) =>
  "org.scala-tools.sbt" %% "scripted-plugin" % version
}
