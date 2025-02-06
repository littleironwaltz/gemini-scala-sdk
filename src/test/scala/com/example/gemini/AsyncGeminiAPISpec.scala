package com.example.gemini

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import sttp.client3.testing._
import io.circe.syntax._
import scala.concurrent.ExecutionContext
import sttp.model.StatusCode

class AsyncGeminiAPISpec extends AsyncWordSpec with Matchers {
  implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockBackend = SttpBackendStub.asynchronousFuture

  // Sample JSON responses for testing
  private val sampleModelListJson = ModelList(Seq(ModelInfo("models/gemini-2.0-test", "Gemini Test Model", "A test model", 4096, 2048))).asJson
  private val sampleModelInfoJson = ModelInfo("models/gemini-2.0-test", "Gemini Test Model", "A test model", 4096, 2048).asJson
  private val sampleGenerateJson = GenerateContentResponse(
    candidates = Seq(Candidate(Content(Seq(Part("London")))))
  ).asJson
  private val sampleTokenCountJson = TokenCountResponse(3).asJson

  // Stubbed backend to simulate API responses
  private val stubbedBackend = mockBackend
    .whenRequestMatches(req => req.method.method == "GET" && req.uri.path.endsWith(List("models")))
    .thenRespond(sampleModelListJson.toString())
    .whenRequestMatches(req => req.method.method == "GET" && req.uri.toString.contains("models%2Fgemini-2.0-test") && !req.uri.path.lastOption.exists(p => p.endsWith(":generateContent") || p.endsWith(":countTokens")))
    .thenRespond(sampleModelInfoJson.toString())
    .whenRequestMatches(req => req.method.method == "POST" && req.uri.path.lastOption.exists(_.endsWith(":generateContent")))
    .thenRespond(sampleGenerateJson.toString())
    .whenRequestMatches(req => req.method.method == "POST" && req.uri.path.lastOption.exists(_.endsWith(":countTokens")))
    .thenRespond(sampleTokenCountJson.toString())

  private val mockApi = new AsyncGeminiAPI()(ec, stubbedBackend)
  val testApiKey = "MOCK_API_KEY"

  "AsyncGeminiAPI (mocked)" should {
    // Test for retrieving models
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

  "AsyncGeminiAPI error handling (mocked)" should {
    // Test for handling HTTP errors
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

  "AsyncGeminiAPI resource management" should {
    // Test for closing backend without errors
    "close backend without errors" in {
      noException should be thrownBy mockApi.closeBackend()
      succeed
    }
  }
}
