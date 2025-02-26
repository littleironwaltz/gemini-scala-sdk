package com.example.gemini

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.BeforeAndAfterAll
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import scala.concurrent.ExecutionContext

class AsyncGeminiAPIIntegrationSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {
  implicit val ec: ExecutionContext = ExecutionContext.global
  private val backend = AsyncHttpClientFutureBackend()
  private val api = new AsyncGeminiAPI()(ec, backend)
  private val targetModel = "gemini-1.5-pro-001"

  // Get API key from environment
  private val apiKey = sys.env.getOrElse("APP_GEMINI_API_KEY", "")

  // Skip all tests if API key is not available
  override def withFixture(test: NoArgAsyncTest) = {
    if (apiKey.isEmpty) {
      info("Skipping integration tests: APP_GEMINI_API_KEY not set")
      cancel("Integration tests require APP_GEMINI_API_KEY to be set")
    } else {
      super.withFixture(test)
    }
  }

  "AsyncGeminiAPI Integration" should {
    "list available models" in {
      api.getModels(apiKey).map {
        case Right(modelList) =>
          modelList.models should not be empty
          info(s"Available models: ${modelList.models.map(_.name).mkString(", ")}")
          modelList.models.exists(_.name == s"models/$targetModel") shouldBe true
          succeed
        case Left(error) =>
          fail(s"Failed to list models: $error")
      }
    }

    "retrieve model details" in {
      api.getModelDetails(targetModel, apiKey).map {
        case Right(modelInfo) =>
          modelInfo.name should include(targetModel)
          succeed
        case Left(error) =>
          fail(s"Failed to get model info: $error")
      }
    }

    "generate content" in {
      api.generateContent(targetModel, "What is the capital of England?", None, apiKey).map {
        case Right(response) =>
          response.candidates.head.content.parts.head.text should include("London")
          succeed
        case Left(error) =>
          fail(s"Failed to generate content: $error")
      }
    }

    "count tokens" in {
      api.countTokens(targetModel, "Hello, world!", apiKey).map {
        case Right(response) =>
          response.totalTokens should be > 0
          succeed
        case Left(error) =>
          fail(s"Failed to count tokens: $error")
      }
    }
  }

  // Cleanup
  override def afterAll(): Unit = {
    api.closeBackend()
    super.afterAll()
  }
}
