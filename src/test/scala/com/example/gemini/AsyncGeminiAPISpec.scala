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
import sttp.client3.testing._
import io.circe.syntax._
import scala.concurrent.ExecutionContext
import sttp.model.StatusCode

class AsyncGeminiAPISpec extends AsyncWordSpec with Matchers {
  implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockBackend = SttpBackendStub.asynchronousFuture

  // モックレスポンスの定義
  // テスト用の構造化されたJSONレスポンス
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

  // 成功系テスト
  // 各APIメソッドの正常系動作を検証
  "AsyncGeminiAPI (mocked)" should {
    // モデル一覧取得の正常系テスト
    // 期待動作: 利用可能なモデルのリストが正しく返される
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

  // エラーハンドリングテスト
  // 各種エラー状況での適切な処理を検証
  "AsyncGeminiAPI error handling (mocked)" should {
    // HTTP 500エラー時の処理検証
    // 期待動作: 適切なエラーメッセージが返され、エラー状態が正しく伝播する
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

  // リソース管理テスト
  // バックエンドとスレッドプールの適切なクリーンアップを検証
  "AsyncGeminiAPI resource management" should {
    // バックエンドのクリーンアップ検証
    // 期待動作: バックエンドが正常にクローズされ、リソースが解放される
    "close backend without errors" in {
      noException should be thrownBy mockApi.closeBackend()
      succeed
    }
  }
}
