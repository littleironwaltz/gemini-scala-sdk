package com.example.gemini

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import scala.util.{Try, Failure}

object ConfigLoader extends LazyLogging {
  private val config: Config = ConfigFactory.load() // Load configuration from application.conf

  // Retrieve a string value from the config, with a default fallback
  private def getString(path: String, default: String): String = {
    Try(config.getString(path)).getOrElse {
      logger.warn(s"Config key '$path' not found or invalid. Falling back to default: '$default'")
      default
    }
  }

  // Retrieve an integer value from the config, with a default fallback
  private def getInt(path: String, default: Int): Int = {
    val valueTry = Try(config.getInt(path))
    valueTry match {
      case scala.util.Success(value) => value
      case Failure(_) =>
        logger.warn(s"Config key '$path' not found or invalid. Falling back to default: '$default'")
        default
    }
  }

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
