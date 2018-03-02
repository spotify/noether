import sbt.Keys._

val commonLibraryDependencies = Seq(
  "org.slf4j" % "slf4j-simple" % "1.7.5",
  "org.scalanlp" %% "breeze" % "0.13.2",
  "org.scalanlp" %% "breeze-natives" % "0.13.2",
  "com.twitter" %% "util-collection" % "18.2.0",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "com.twitter" %% "algebird-core" % "0.13.3"
)

val commonSettings = Seq(
  organization := "com.spotify.ml.aggregators",
  scalaVersion := "2.11.9",
  version := "0.1.0-SNAPSHOT",
  fork := true,
  libraryDependencies ++= commonLibraryDependencies,
  resolvers += Resolver.mavenLocal
)

lazy val root: Project = Project("aggregators-root",file(".")).aggregate(core)
lazy val core : Project = project.in(file("aggregators")).settings(commonSettings)