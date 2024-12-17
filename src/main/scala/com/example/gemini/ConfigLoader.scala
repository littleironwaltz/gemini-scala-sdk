package com.example.gemini

import com.typesafe.config.ConfigFactory

/**
 * Configuration loader for the Gemini API client.
 * Loads and provides access to application configuration values from application.conf.
 * This object handles the centralized configuration management for the API client.
 */
object ConfigLoader {
  /**
   * The loaded configuration instance from application.conf
   */
  private val config = ConfigFactory.load()

  /**
   * The API key for authenticating with the Gemini API.
   * Loaded from the configuration path "app.gemini.apiKey"
   */
  lazy val apiKey: String = config.getString("app.gemini.apiKey")

  /**
   * The base URL for the Gemini API endpoints.
   * Loaded from the configuration path "app.gemini.baseUrl"
   */
  lazy val baseUrl: String = config.getString("app.gemini.baseUrl")
}
