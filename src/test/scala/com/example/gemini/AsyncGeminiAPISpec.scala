/*
 * Unit Tests for AsyncGeminiAPI
 *
 * Provides comprehensive test coverage for the Gemini API client:
 * - Success case testing for all API operations
 * - Error handling verification
 * - Resource management testing
 * - Mock response handling
 *
 * Uses ScalaTest AsyncWordSpec for asynchronous testing
 * with SttpBackendStub for HTTP request mocking.
 */

package com.example.gemini

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{BeforeAndAfterAll, TestSuite}
import sttp.client3.testing._
import io.circe.syntax._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import sttp.model.StatusCode
import java.util.concurrent.TimeUnit

class AsyncGeminiAPISpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with TestSuite {
  private val threadPool = java.util.concurrent.Executors.newFixedThreadPool(4)
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(threadPool)

  private val mockBackend = SttpBackendStub.asynchronousFuture

  // Mock response definitions
  // Structured JSON responses for testing
  private val sampleModelListJson = ModelList(
    Seq(ModelInfo(
      name = "models/gemini-2.0-test",
      displayName = "Gemini Test Model",
      description = "A test model",
      inputTokenLimit = 4096,
      outputTokenLimit = 2048
    ))
  ).asJson

  private val sampleModelInfoJson = ModelInfo(
    name = "models/gemini-2.0-test",
    displayName = "Gemini Test Model",
    description = "A test model",
    inputTokenLimit = 4096,
    outputTokenLimit = 2048
  ).asJson

  private val sampleGenerateJson = GenerateContentResponse(
    candidates = Seq(
      Candidate(
        content = Content(
          parts = Seq(Part(text = "London"))
        )
      )
    )
  ).asJson

  private val sampleTokenCountJson = TokenCountResponse(
    totalTokens = 3
  ).asJson

  // Stubbed backend to simulate API responses
  private val stubbedBackend = mockBackend
    // List models endpoint
    .whenRequestMatches(req => 
      req.method.method == "GET" && 
      req.uri.path.endsWith(List("models"))
    ).thenRespond(sampleModelListJson.toString())
    // Model details endpoint
    .whenRequestMatches(req => 
      req.method.method == "GET" && 
      req.uri.path.endsWith(List("models", "gemini-2.0-test"))
    ).thenRespond(sampleModelInfoJson.toString())
    // Generate content endpoint
    .whenRequestMatches(req => 
      req.method.method == "POST" && 
      req.uri.path.endsWith(List("models", "gemini-2.0-test", ":generateContent"))
    ).thenRespond(sampleGenerateJson.toString())
    // Count tokens endpoint
    .whenRequestMatches(req => 
      req.method.method == "POST" && 
      req.uri.path.endsWith(List("models", "gemini-2.0-test", ":countTokens"))
    ).thenRespond(sampleTokenCountJson.toString())

  private val mockApi = new AsyncGeminiAPI()(ec, stubbedBackend)
  val testApiKey = "MOCK_API_KEY"

  // Success case tests
  // Verify normal operation of each API method
  "AsyncGeminiAPI (mocked)" should {
    // Test for retrieving model list
    // Expected: Returns a list of available models correctly
    "retrieve models successfully" in {
      mockApi.getModels(testApiKey).map {
        case Right(modelList) =>
          modelList.models should not be empty
          modelList.models.head.name shouldBe "models/gemini-2.0-test"
        case Left(error) =>
          fail(s"Expected model list, got error: ${error.message}")
      }
    }

    // Test for retrieving model details
    "retrieve model details successfully" in {
      mockApi.getModelDetails("models/gemini-2.0-test", testApiKey).map {
        case Right(info) =>
          info.name should include("gemini")
        case Left(error) =>
          fail(s"Expected model details, got error: ${error.message}")
      }
    }

    // Test for generating content
    "generate content successfully" in {
      mockApi.generateContent("models/gemini-2.0-test", "What is the capital of France?", None, testApiKey).map {
        case Right(response) =>
          response.candidates should not be empty
          response.candidates.head.content.parts.head.text shouldBe "London"
        case Left(error) =>
          fail(s"Expected successful generation, got error: ${error.message}")
      }
    }

    // Test for counting tokens
    "count tokens successfully" in {
      mockApi.countTokens("models/gemini-2.0-test", "Hello world", testApiKey).map {
        case Right(tokenCount) =>
          tokenCount.totalTokens should be > 0
        case Left(error) =>
          fail(s"Expected token count, got error: ${error.message}")
      }
    }
  }

  // Error handling tests
  // Verify proper handling of various error conditions
  "AsyncGeminiAPI error handling (mocked)" should {
    // Test handling of HTTP 500 errors
    // Expected: Appropriate error message is returned and error state propagates correctly
    "handle HTTP errors properly" in {
      val errorBackend = mockBackend.whenAnyRequest.thenRespondWithCode(StatusCode.InternalServerError, "Internal Server Error")
      val apiWithError = new AsyncGeminiAPI()(ec, errorBackend)

      apiWithError.getModels(testApiKey).map {
        case Left(err) =>
          err.message should include("HTTP error: 500")
        case Right(_) =>
          fail("Expected HTTP error but got success")
      }
    }

    // Test for handling JSON deserialization errors
    "handle JSON deserialization errors properly" in {
      val invalidJsonBackend = mockBackend.whenAnyRequest.thenRespond("{ invalid_json }")
      val apiWithInvalidJson = new AsyncGeminiAPI()(ec, invalidJsonBackend)

      apiWithInvalidJson.getModels(testApiKey).map {
        case Left(err) =>
          err.message should include("Deserialization error")
        case Right(_) =>
          fail("Expected JSON deserialization error but got success")
      }
    }
  }

  "AsyncGeminiAPI with no API key" should {
    // Test for authentication failure
    "fail to authenticate (simulated)" in {
      val noAuthBackend = mockBackend.whenAnyRequest.thenRespondWithCode(StatusCode.Unauthorized, "Unauthorized")
      val apiNoAuth = new AsyncGeminiAPI()(ec, noAuthBackend)

      apiNoAuth.getModels("INVALID_KEY").map {
        case Left(err) =>
          err.message should include("HTTP error: 401")
        case Right(_) =>
          fail("Expected authentication error but got success")
      }
    }
  }

  // Resource management tests
  // Verify proper cleanup of backend and thread pool
  "AsyncGeminiAPI resource management" should {
    // Test backend cleanup
    // Expected: Backend closes normally and resources are released
    "close backend without errors" in {
      noException should be thrownBy mockApi.closeBackend()
      succeed
    }
  }

  override def afterAll(): Unit = {
    try {
      mockApi.closeBackend()
      threadPool.shutdown()
      if (!threadPool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
        threadPool.shutdownNow()
      }
    } finally {
      super.afterAll()
    }
  }
}
