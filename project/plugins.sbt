addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.3")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.0")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.8.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.4.1")
addSbtPlugin("com.github.gseitz" % "sbt-protobuf" % "0.6.5")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.23")
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.5")
addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.4.6")

libraryDependencies ++= Seq(
  "com.github.os72" % "protoc-jar" % "3.11.4"
)
