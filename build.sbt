// Set the organization name
organization := "com.example.gemini"

// Set the project name
name := "gemini-scala-sdk"

// Set the project version
version := "1.0.0"

// Set the Scala version for the build
ThisBuild / scalaVersion := "2.13.12"

// Compiler options for scalac
scalacOptions ++= Seq(
  "-Ymacro-annotations", // Enable macro annotations
  "-deprecation",        // Emit warning and location for usages of deprecated APIs
  "-feature",            // Emit warning and location for usages of features that should be imported explicitly
  "-unchecked",          // Enable additional warnings where generated code depends on assumptions
  "-Xlint:unused",       // Warn when local and private vals, vars, defs, and types are unused
  "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver
  "-Xlint:inaccessible", // Warn about inaccessible types in method signatures
  "-Xlint:infer-any",    // Warn when a type argument is inferred to be `Any`
  "-Werror"              // Fail the compilation if there are any warnings
)

// Define library versions
val SttpVersion      = "3.9.1"
val CirceVersion     = "0.14.1"
val ConfigVersion    = "1.4.2"
val LoggingVersion   = "3.9.5"
val LogbackVersion   = "1.2.11"
val ScalaTestVersion = "3.2.16"

// Add library dependencies
libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client3" %% "core" % SttpVersion, // STTP core library
  "com.softwaremill.sttp.client3" %% "circe" % SttpVersion, // STTP Circe integration
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % SttpVersion, // STTP async HTTP client backend

  "io.circe" %% "circe-generic" % CirceVersion, // Circe generic support
  "io.circe" %% "circe-parser" % CirceVersion, // Circe JSON parser
  "io.circe" %% "circe-generic-extras" % CirceVersion, // Circe generic extras

  "com.typesafe" % "config" % ConfigVersion, // Typesafe Config library
  "com.typesafe.scala-logging" %% "scala-logging" % LoggingVersion, // Scala logging library

  "org.scalatest" %% "scalatest" % ScalaTestVersion % "test", // ScalaTest for testing

  "ch.qos.logback" % "logback-classic" % LogbackVersion // Logback for logging
)
