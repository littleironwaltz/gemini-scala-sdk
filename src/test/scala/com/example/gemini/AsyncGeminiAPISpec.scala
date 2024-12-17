package com.example.gemini

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import scala.concurrent.Future

/**
 * Integration test suite for AsyncGeminiAPI.
 * Tests the actual interaction with the Gemini API endpoints.
 *
 * Important Notes:
 * - These tests make real API calls to the Gemini service
 * - A valid API key must be configured in application.conf
 * - For production environments, consider mocking the HTTP responses instead
 */
class AsyncGeminiAPISpec extends AsyncWordSpec with Matchers {

  val apiKey = ConfigLoader.apiKey

  "AsyncGeminiAPI" should {
    /**
     * Tests the model listing functionality.
     * Verifies that the API can successfully retrieve a list of available models.
     */
    "retrieve models successfully" in {
      AsyncGeminiAPI.getModels(apiKey).map {
        case Right(modelList) =>
          modelList.models should not be empty
        case Left(error) =>
          fail(s"Expected successful model list, but got error: ${error.message}")
      }
    }

    /**
     * Tests the model details retrieval functionality.
     * Verifies that the API can fetch detailed information about a specific model.
     */
    "retrieve model details successfully" in {
      val testModelName = "models/gemini-2.0-flash-exp"
      AsyncGeminiAPI.getModelDetails(testModelName, apiKey).map {
        case Right(info) =>
          info.name should include("gemini")
        case Left(error) =>
          fail(s"Expected successful model details, but got error: ${error.message}")
      }
    }

    /**
     * Tests the content generation functionality.
     * Verifies that the API can generate content from a given prompt.
     */
    "generate content" in {
      val prompt = "What is the capital of France?"
      AsyncGeminiAPI.generateContent("models/gemini-2.0-flash-exp", prompt, None, apiKey).map {
        case Right(response) =>
          response.candidates should not be empty
        case Left(error) =>
          fail(s"Expected successful generation, but got error: ${error.message}")
      }
    }

    /**
     * Tests the token counting functionality.
     * Verifies that the API can accurately count tokens in a given text.
     */
    "count tokens" in {
      val text = "Hello world"
      AsyncGeminiAPI.countTokens("models/gemini-2.0-flash-exp", text, apiKey).map {
        case Right(tokenCount) =>
          tokenCount.totalTokens should be > 0
        case Left(error) =>
          fail(s"Expected token count, but got error: ${error.message}")
      }
    }
  }
}
