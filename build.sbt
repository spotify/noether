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

val slf4jVersion = "1.7.5"
val breezeVersion = "0.13.2"
val twitterUtil = "18.2.0"
val algebirdVersion = "0.13.3"
val scalaTestVersion = "3.0.4"

val commonSettings = Seq(
  organization := "com.spotify.ml",
  name := "aggregators",
  description := "ML Aggregators",
  scalaVersion := "2.11.12",
  crossScalaVersions := Seq("2.11.12", "2.12.4"),
  scalacOptions ++= Seq("-target:jvm-1.8", "-deprecation", "-feature", "-unchecked"),
  scalacOptions in (Compile, doc) ++= Seq("-skip-packages", "org.apache"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked"),
  javacOptions in (Compile, doc)  := Seq("-source", "1.8"),

  // Release settings
  publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging),
  releaseCrossBuild             := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle             := true,
  publishArtifact in Test       := false,
  sonatypeProfileName           := "com.spotify",
  licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
)

val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val root: Project = Project("root",file(".")
).settings(commonSettings ++ noPublishSettings).aggregate(aggregators)  

lazy val aggregators: Project = Project(
  "aggregators",
  file("aggregators")
).settings(
  commonSettings,
  moduleName := "ml-aggregators",
  description := "Machine Learning Aggregators",
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-simple" % slf4jVersion,
    "org.scalanlp" %% "breeze" % breezeVersion,
    "org.scalanlp" %% "breeze-natives" % breezeVersion,
    "com.twitter" %% "util-collection" % twitterUtil,  
    "com.twitter" %% "algebird-core" % algebirdVersion,
    "org.scalatest" %% "scalatest" %  scalaTestVersion
  )
)