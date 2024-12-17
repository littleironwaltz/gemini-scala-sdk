package com.example.gemini

import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await}
import scala.concurrent.duration._

object TestAsyncGeminiSDK extends App with LazyLogging {
  val apiKey = ConfigLoader.apiKey
  if (apiKey == "YOUR_API_KEY") {
    logger.warn("No real API key provided. Set APP_GEMINI_API_KEY or -Dapp.gemini.apiKey before running.")
  }

  val api = AsyncGeminiAPI.default

  // Log the result of fetching models
  private def logModelsResult(modelsResult: Either[GeminiError, ModelList]): Unit = {
    modelsResult match {
      case Right(modelList) =>
        logger.info("Available Models:")
        modelList.models.foreach { m =>
          logger.info(s"- ${m.name}: ${m.displayName}")
        }
      case Left(err) =>
        logger.error(s"Error fetching models: ${err.message}")
    }
  }

  // Log the result of fetching model details
  private def logModelDetailsResult(modelDetailsResult: Either[GeminiError, ModelInfo]): Unit = {
    modelDetailsResult match {
      case Right(info) =>
        logger.info("Model Details:")
        logger.info(s"Name: ${info.name}")
        logger.info(s"DisplayName: ${info.displayName}")
        logger.info(s"Description: ${info.description}")
      case Left(err) =>
        logger.error(s"Error fetching model details: ${err.message}")
    }
  }

  // Log the result of generating content
  private def logGeneratedContentResult(contentResult: Either[GeminiError, GenerateContentResponse]): Unit = {
    contentResult match {
      case Right(contentRes) =>
        val texts = contentRes.candidates.flatMap(_.content.parts.map(_.text))
        logger.info("Generated Content:")
        texts.foreach(line => logger.info(line))
      case Left(err) =>
        logger.error(s"Error generating content: ${err.message}")
    }
  }

  // Log the result of counting tokens
  private def logTokenCountResult(countResult: Either[GeminiError, TokenCountResponse]): Unit = {
    countResult match {
      case Right(tokenRes) =>
        logger.info(s"Token Count: ${tokenRes.totalTokens}")
      case Left(err) =>
        logger.error(s"Error counting tokens: ${err.message}")
    }
  }

  logger.info("=== Get Models ===")
  val modelsFuture = api.getModels(apiKey).map(logModelsResult)

  val modelName = "models/gemini-2.0-flash-exp"
  val detailsFuture = modelsFuture.flatMap { _ =>
    logger.info("\n=== Get Model Details ===")
    api.getModelDetails(modelName, apiKey).map(logModelDetailsResult)
  }

  val prompt = "What is the capital of the United Kingdom?"
  val generateFuture = detailsFuture.flatMap { _ =>
    logger.info("\n=== Generate Content ===")
    api.generateContent(modelName, prompt, None, apiKey).map(logGeneratedContentResult)
  }

  val countFuture = generateFuture.flatMap { _ =>
    logger.info("\n=== Count Tokens ===")
    api.countTokens(modelName, "Hello world", apiKey).map(logTokenCountResult)
  }

  // Ensure completion of all futures before exiting
  val result = Await.ready(countFuture, 60.seconds) // Set timeout as needed

  logger.info("All demonstrations completed. Cleaning up resources.")
  api.closeBackend()
}
