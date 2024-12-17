/**
 * Dependencies for the Gemini Scala SDK
 * 
 * Core Dependencies:
 * - sttp client3: HTTP client library for API communication
 * - circe: JSON serialization/deserialization
 * - typesafe config: Configuration management
 * - scala-logging: Logging functionality
 */
libraryDependencies ++= Seq(
  // HTTP client and JSON support
  "com.softwaremill.sttp.client3" %% "core" % "3.9.1",
  "com.softwaremill.sttp.client3" %% "circe" % "3.9.1",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % "3.9.1",

  // JSON processing
  "io.circe" %% "circe-generic" % "0.14.1",
  "io.circe" %% "circe-parser" % "0.14.1",
  "io.circe" %% "circe-generic-extras" % "0.14.1",

  // Configuration and logging
  "com.typesafe" % "config" % "1.4.2",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",

  // Testing
  "org.scalatest" %% "scalatest" % "3.2.16" % "test",

  // Logging implementation
  "ch.qos.logback" % "logback-classic" % "1.2.11"
)

/**
 * Scala version and compiler options
 */
ThisBuild / scalaVersion := "2.13.12"

/**
 * Enable macro annotations support
 * Required for circe's automatic derivation
 */
scalacOptions += "-Ymacro-annotations"
