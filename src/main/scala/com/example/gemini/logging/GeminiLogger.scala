package com.example.gemini.logging

import com.typesafe.scalalogging.LazyLogging
import com.example.gemini.GeminiError

trait GeminiLogger extends LazyLogging {
  protected def logRequest(method: String, path: String): Unit =
    logger.debug(s"[$method] $path")
    
  protected def logError(context: String, error: GeminiError): Unit = {
    val sanitizedMessage = error.message.replaceAll("key=[^&]*", "key=REDACTED")
    logger.error(s"[$context] $sanitizedMessage")
  }
}
