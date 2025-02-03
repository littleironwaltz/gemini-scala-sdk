package com.example.gemini

import sttp.client3._
import sttp.client3.circe._
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import io.circe.generic.auto._
import io.circe.Error
import scala.concurrent.{Future, ExecutionContext, ExecutionContextExecutorService}
import com.typesafe.scalalogging.LazyLogging
import java.util.concurrent.Executors

class AsyncGeminiAPI(
    implicit val ec: ExecutionContext,
    backend: SttpBackend[Future, Any]
) extends LazyLogging {

  type GeminiResult[T] = Future[Either[GeminiError, T]]
  type GeminiResponse[T] = Response[Either[ResponseException[String, Error], T]]

  private val baseUrl = ConfigLoader.baseUrl

  private def normalizeModelName(modelName: String): String = {
    val prefix = "models/"
    if (modelName.startsWith(prefix)) modelName.stripPrefix(prefix)
    else modelName
  }

  private def buildGetRequest[T: Decoder](path: String, apiKey: String): Request[Either[ResponseException[String, Error], T]] = {
    basicRequest
      .get(uri"$baseUrl/$path?key=$apiKey")
      .response(asJson[T])
  }

  private def buildPostRequest[T: Decoder, B: Encoder](path: String, body: B, apiKey: String): Request[Either[ResponseException[String, Error], T]] = {
    basicRequest
      .post(uri"$baseUrl/$path?key=$apiKey")
      .header("Content-Type", "application/json")
      .body(body)
      .response(asJson[T])
  }

  private def logError(context: String, error: GeminiError): Unit = {
    val sanitizedMessage = error.message.replaceAll("key=[^&]*", "key=REDACTED")
    logger.error(s"[$context] $sanitizedMessage")
  }

  private def handleRequest[T](request: Request[Either[ResponseException[String, Error], T]], context: String): GeminiResult[T] = {
    request.send(backend).map { resp =>
      val result = handleResponse(resp, context)
      result.left.foreach(e => logError(context, e))
      result
    }
  }

  // Handle HTTP response and map to either GeminiError or the expected type
  private def handleResponse[T](
      response: GeminiResponse[T],
      requestDescription: String
  ): Either[GeminiError, T] = {
    response.body match {
      case Right(value) => Right(value) // Successful response
      case Left(error) =>
        val statusCode = response.code.code
        val detailedContext = s"Request: $requestDescription, StatusCode: $statusCode"
        error match {
          case HttpError(body, _) =>
            Left(HttpErrorStatus(statusCode, s"$detailedContext, Body: $body")) // HTTP error
          case DeserializationException(original, ex) =>
            Left(JsonDeserializationError(original, s"$detailedContext, Cause: ${ex.getMessage}")) // JSON deserialization error
        }
    }
  }

  // Fetch list of models
  def getModels(apiKey: String): GeminiResult[ModelList] = {
    handleRequest(
      buildGetRequest[ModelList]("models", apiKey),
      "GET /models"
    )
  }

  def getModelDetails(modelName: String, apiKey: String): GeminiResult[ModelInfo] = {
    val name = normalizeModelName(modelName)
    handleRequest(
      buildGetRequest[ModelInfo](s"models/$name", apiKey),
      s"GET /models/$name"
    )
  }

  def generateContent(
      modelName: String,
      prompt: String,
      config: Option[GenerationConfig],
      apiKey: String
  ): GeminiResult[GenerateContentResponse] = {
    val name = normalizeModelName(modelName)
    val requestBody = GenerateContentRequest(
      contents = Seq(ContentItem("user", Seq(Part(prompt))))
    )
    handleRequest(
      buildPostRequest[GenerateContentResponse, GenerateContentRequest](
        s"models/$name:generateContent",
        requestBody,
        apiKey
      ),
      s"POST /models/$name:generateContent"
    )
  }

  def countTokens(
      modelName: String,
      text: String,
      apiKey: String
  ): GeminiResult[TokenCountResponse] = {
    val name = normalizeModelName(modelName)
    val requestBody = CountTokensRequest(
      contents = Some(Seq(ContentItem("user", Seq(Part(text))))),
      generateContentRequest = None
    )
    handleRequest(
      buildPostRequest[TokenCountResponse, CountTokensRequest](
        s"models/$name:countTokens",
        requestBody,
        apiKey
      ),
      s"POST /models/$name:countTokens"
    )
  }

  /**
   * Closes the backend and performs cleanup of resources.
   * 
   * This method:
   * 1. Closes the STTP backend to release HTTP client resources
   * 2. If the ExecutionContext is an ExecutionContextExecutorService,
   *    shuts it down to release thread pool resources
   * 
   * Note: External ExecutionContexts are not shut down to prevent
   * affecting other parts of the application.
   */
  def closeBackend(): Unit = {
    backend.close()
    ec match {
      case eces: ExecutionContextExecutorService =>
        logger.info("Shutting down executor service")
        eces.shutdown()
      case _ =>
        logger.debug("ExecutionContext is external; not shutting it down.")
    }
  }
}

object AsyncGeminiAPI {
  private val threadPoolSize = ConfigLoader.threadPoolSize
  private val httpExecutor = Executors.newFixedThreadPool(threadPoolSize)
  implicit private val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(httpExecutor)

  private val backend = AsyncHttpClientFutureBackend()

  val default: AsyncGeminiAPI = new AsyncGeminiAPI()(ec, backend) // Default instance of AsyncGeminiAPI
}
