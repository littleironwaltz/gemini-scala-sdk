/*
 * Configuration Management for Gemini SDK
 * 
 * Handles loading and managing configuration settings from application.conf
 * and environment variables. Provides fallback values and logging for
 * configuration issues.
 * 
 * Key features:
 * - HOCON configuration file support
 * - Environment variable overrides
 * - Type-safe configuration access
 * - Fallback value handling
 * - Configuration validation logging
 */

package com.example.gemini

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import scala.util.{Try, Failure}

object ConfigLoader extends LazyLogging {
  private val config: Config = ConfigFactory.load() // Load configuration from application.conf

  // Retrieve a string value from the config, with a default fallback
  private def getConfigValue[T](
    path: String,
    default: T,
    getter: String => T
  ): T = {
    Try(getter(path)).fold(
      error => {
        logger.warn(s"Config key '$path' not found or invalid. Falling back to default: '$default'", error)
        default
      },
      value => value
    )
  }

  private def getString(path: String, default: String): String =
    getConfigValue(path, default, config.getString)

  private def getInt(path: String, default: Int): Int =
    getConfigValue(path, default, config.getInt)

  // Lazy-loaded configuration values
  lazy val apiKey: String = {
    val key = getString("app.gemini.apiKey", "YOUR_API_KEY")
    if (key == "YOUR_API_KEY") {
      logger.info("Using default API key placeholder.")
    }
    key
  }

  lazy val baseUrl: String = getString("app.gemini.baseUrl", "https://generativelanguage.googleapis.com/v1beta")
  lazy val threadPoolSize: Int = getInt("app.gemini.threadPoolSize", 4)
  lazy val requestTimeoutMillis: Int = getInt("app.gemini.requestTimeoutMillis", 30000)

  // Log the loaded configuration
  logger.debug(
    s"""Loaded configuration:
       |apiKey: ${if (apiKey == "YOUR_API_KEY") "default (placeholder)" else "provided"}
       |baseUrl: $baseUrl
       |threadPoolSize: $threadPoolSize
       |requestTimeoutMillis: $requestTimeoutMillis
       |""".stripMargin
  )
}
