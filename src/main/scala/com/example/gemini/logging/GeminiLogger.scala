package com.example.gemini.logging

import com.typesafe.scalalogging.LazyLogging
import com.example.gemini.GeminiError

/**
 * Common Logging Functionality Trait
 * 
 * Features:
 * - Context-based log output
 * - Automatic API key sanitization
 * - Consistent log formatting
 * - Structured error information output
 */
trait GeminiLogger extends LazyLogging {
  /**
   * Output an info level log message
   * 
   * @param context Log context (e.g., "AsyncGeminiAPI", "ConfigLoader")
   * @param message Log message
   */
  protected def logInfo(context: String, message: String): Unit =
    logger.info(s"[$context] $message")

  /**
   * Output an error level log message (automatically masks API keys)
   * 
   * @param context Log context
   * @param error GeminiError instance
   */
  protected def logError(context: String, error: GeminiError): Unit = {
    val sanitizedMessage = error.message.replaceAll("key=[^&]*", "key=REDACTED")
    logger.error(s"[$context] $sanitizedMessage")
  }

  /**
   * Output a debug level log message
   * 
   * @param context Log context
   * @param message Log message
   */
  protected def logDebug(context: String, message: String): Unit =
    logger.debug(s"[$context] $message")

  /**
   * Output request information log
   * 
   * @param method HTTP method
   * @param path Request path
   */
  protected def logRequest(method: String, path: String): Unit =
    logDebug("Request", s"$method $path")
}
