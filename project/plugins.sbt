addSbtPlugin("com.github.sbt" % "sbt-unidoc" % "0.5.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.7")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.4")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.2")
addSbtPlugin("com.github.sbt" % "sbt-ghpages" % "0.7.0")
addSbtPlugin("com.github.sbt" % "sbt-site" % "1.5.0")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.4")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")
addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.5.5")

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin" % "0.11.13"
)

dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "2.1.0"
