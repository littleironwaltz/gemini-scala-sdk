package com.example.gemini

import scala.concurrent.Await
import scala.concurrent.duration._
import com.typesafe.scalalogging.LazyLogging

/**
 * Demonstration application for the Gemini API SDK.
 * This object showcases the basic usage of AsyncGeminiAPI by executing
 * various API calls and displaying their results.
 *
 * Prerequisites:
 *  - A valid API key must be configured in application.conf (app.gemini.apiKey)
 *  - Internet connectivity to access the Gemini API endpoints
 *
 * Note: This example uses Await.result for simplicity in demonstration.
 * In production applications, consider using proper Future handling instead.
 */
object TestAsyncGeminiSDK extends App with LazyLogging {
  /**
   * Load API key from configuration
   */
  val apiKey = ConfigLoader.apiKey

  /**
   * Demonstration 1: List Available Models
   * Retrieves and displays all available Gemini models
   */
  logger.info("=== Get Models ===")
  val modelsFuture = AsyncGeminiAPI.getModels(apiKey)
  val modelsResult = Await.result(modelsFuture, 30.seconds)
  modelsResult match {
    case Right(modelList) =>
      logger.info("Available Models:")
      modelList.models.foreach { m =>
        logger.info(s"- ${m.name}: ${m.displayName}")
      }
    case Left(err) =>
      logger.error(s"Error fetching models: ${err.message}")
  }

  /**
   * Demonstration 2: Get Model Details
   * Retrieves and displays detailed information about a specific model
   */
  logger.info("\n=== Get Model Details ===")
  val modelDetailsFuture = AsyncGeminiAPI.getModelDetails("models/gemini-2.0-flash-exp", apiKey)
  val modelDetailsResult = Await.result(modelDetailsFuture, 30.seconds)
  modelDetailsResult match {
    case Right(modelInfo) =>
      logger.info("Model Details:")
      logger.info(s"Name: ${modelInfo.name}")
      logger.info(s"DisplayName: ${modelInfo.displayName}")
      logger.info(s"Description: ${modelInfo.description}")
    case Left(err) =>
      logger.error(s"Error fetching model details: ${err.message}")
  }

  /**
   * Demonstration 3: Generate Content
   * Sends a simple prompt to the model and displays the generated response
   */
  logger.info("\n=== Generate Content ===")
  val prompt = "What is the capital of the United Kingdom?"
  val contentFuture = AsyncGeminiAPI.generateContent("models/gemini-2.0-flash-exp", prompt, None, apiKey)
  val contentResult = Await.result(contentFuture, 30.seconds)
  contentResult match {
    case Right(contentRes) =>
      val texts = contentRes.candidates.flatMap(_.content.parts.map(_.text))
      logger.info(s"Generated Content:\n${texts.mkString("\n")}")
    case Left(err) =>
      logger.error(s"Error generating content: ${err.message}")
  }

  /**
   * Demonstration 4: Count Tokens
   * Shows how to count tokens in a text string using the model
   */
  logger.info("\n=== Count Tokens ===")
  val countTokensFuture = AsyncGeminiAPI.countTokens("models/gemini-2.0-flash-exp", "Hello world", apiKey)
  val countTokensResult = Await.result(countTokensFuture, 30.seconds)
  countTokensResult match {
    case Right(tokenRes) =>
      logger.info(s"Token Count: ${tokenRes.totalTokens}")
    case Left(err) =>
      logger.error(s"Error counting tokens: ${err.message}")
  }

  /**
   * Cleanup: Release resources
   * Ensures proper cleanup of the API client resources
   */
  AsyncGeminiAPI.closeBackend()
}
