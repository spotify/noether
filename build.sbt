/*
 * Copyright 2018 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import sbt.Keys._

val breezeVersion = "1.0-RC2"
val algebirdVersion = "0.13.4"
val scalaTestVersion = "3.0.5"

val commonSettings = Seq(
  organization := "com.spotify.ml",
  name := "noether",
  description := "ML Aggregators",
  scalaVersion := "2.11.12",
  crossScalaVersions := Seq("2.11.12", "2.12.5"),
  scalacOptions ++= Seq("-target:jvm-1.8", "-deprecation", "-feature", "-unchecked"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked"),
  javacOptions in (Compile, doc)  := Seq("-source", "1.8"),
  publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging),
  releaseCrossBuild             := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle             := true,
  publishArtifact in Test       := false,
  sonatypeProfileName           := "com.spotify",
  licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/spotify/noether")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/spotify/noether.git"),
    "scm:git:git@github.com:spotify/noether.git")),
  developers := List(
    Developer(id="rwhitcomb", name="Richard Whitcomb", email="richwhitjr@gmail.com", url=url("https://twitter.com/rwhitcomb")),
    Developer(id="fallonfofallon", name="Fallon Chen", email="fallon@spotify.com", url=url("https://twitter.com/fallonfofallon"))
  )
)

val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val root: Project = Project("root",file(".")
).settings(commonSettings ++ noPublishSettings).aggregate(core)

lazy val core: Project = Project(
  "core",
  file("core")
).settings(
  commonSettings,
  moduleName := "noether-core",
  description := "Machine Learning Aggregators",
  libraryDependencies ++= Seq(
    "org.scalanlp" %% "breeze" % breezeVersion,
    "com.twitter" %% "algebird-core" % algebirdVersion,
    "org.scalatest" %% "scalatest" %  scalaTestVersion
  )
)
