import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import pl.project13.scala.sbt.JmhPlugin

import BuildHelper.*

ThisBuild / scalaVersion := "3.3.5"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val stdSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Werror"
  ),
  libraryDependencies += "dev.zio" %% "zio" % "2.1.16"
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "profiling",
    publish / skip := true
  )
  .aggregate(benchmarks)

lazy val benchmarks = project
  .in(file("benchmarks"))
  .enablePlugins(JmhPlugin)
  .settings(stdSettings)
  .settings(replSettings)
  .settings(
    name := "profiling-benchmarks",
    publish / skip := true,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-profiling-jmh" % "0.3.2" % Test
    ),
//    libraryDependencies += "dev.zio" %% "zio-profiling" % "0.3.2",
//    libraryDependencies += compilerPlugin("dev.zio" %% "zio-profiling-tagging-plugin" % "0.3.2"),
    excludeDependencies ++= {
      if (scalaVersion.value == "3.3.5") List(ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"))
      else Nil
    },
    Compile / console / scalacOptions := Seq(
      "-language:higherKinds",
      "-language:existentials",
      "-Xsource:2.13",
      "-Yrepl-class-based"
    ),
    javacOptions ++= Seq("--release", "21"), // Compile for Java 21
    javaOptions ++= Seq("--enable-preview"), // If using preview features (optional)
    fork := true // Ensure benchmarks run in a separate JVM
  )
  .settings(scalacOptions += "-Wconf:msg=[@nowarn annotation does not suppress any warnings]:silent")
  .settings(
    assembly / assemblyJarName := "benchmarks.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("module-info.class") => MergeStrategy.discard
      case path                          => MergeStrategy.defaultMergeStrategy(path)
    },
    assembly / fullClasspath := (Jmh / fullClasspath).value,
    assembly / mainClass     := Some("org.openjdk.jmh.Main")
  )

