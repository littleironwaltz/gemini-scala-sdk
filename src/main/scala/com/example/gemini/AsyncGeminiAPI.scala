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

  private val baseUrl = ConfigLoader.baseUrl

  // Handle HTTP response and map to either GeminiError or the expected type
  private def handleResponse[T](
      response: Response[Either[ResponseException[String, Error], T]],
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
  def getModels(apiKey: String): Future[Either[GeminiError, ModelList]] = {
    val requestDescription = "GET /models"
    val request = basicRequest
      .get(uri"$baseUrl/models?key=$apiKey")
      .response(asJson[ModelList])

    request.send(backend).map { resp =>
      val result = handleResponse(resp, requestDescription)
      result.left.foreach(e => logger.error(s"[getModels] ${e.message}")) // Log error if any
      result
    }
  }

  // Fetch details of a specific model
  def getModelDetails(modelName: String, apiKey: String): Future[Either[GeminiError, ModelInfo]] = {
    val pureModelName = modelName.stripPrefix("models/")
    val requestDescription = s"GET /models/$pureModelName"
    val request = basicRequest
      .get(uri"$baseUrl/models/$pureModelName?key=$apiKey")
      .response(asJson[ModelInfo])

    request.send(backend).map { resp =>
      val result = handleResponse(resp, requestDescription)
      result.left.foreach(e => logger.error(s"[getModelDetails] ${e.message}")) // Log error if any
      result
    }
  }

  // Generate content using a model
  def generateContent(
      modelName: String,
      prompt: String,
      config: Option[GenerationConfig],
      apiKey: String
  ): Future[Either[GeminiError, GenerateContentResponse]] = {
    val pureModelName = modelName.stripPrefix("models/")
    val requestBody = GenerateContentRequest(
      contents = Seq(
        ContentItem(
          role = "user",
          parts = Seq(
            Part(text = prompt)
          )
        )
      )
    )
    val requestDescription = s"POST /models/$pureModelName:generateContent"

    val request = basicRequest
      .post(uri"$baseUrl/models/$pureModelName:generateContent?key=$apiKey")
      .header("Content-Type", "application/json")
      .body(requestBody)
      .response(asJson[GenerateContentResponse])

    request.send(backend).map { resp =>
      val result = handleResponse(resp, requestDescription)
      result.left.foreach(e => logger.error(s"[generateContent] ${e.message}")) // Log error if any
      result
    }
  }

  // Count tokens in a given text
  def countTokens(
      modelName: String,
      text: String,
      apiKey: String
  ): Future[Either[GeminiError, TokenCountResponse]] = {
    val pureModelName = modelName.stripPrefix("models/")
    val requestDescription = s"POST /models/$pureModelName:countTokens"
    val requestBody = CountTokensRequest(
      contents = Some(Seq(
        ContentItem(
          role = "user",
          parts = Seq(Part(text = text))
        )
      )),
      generateContentRequest = None
    )

    val request = basicRequest
      .post(uri"$baseUrl/models/$pureModelName:countTokens?key=$apiKey")
      .header("Content-Type", "application/json")
      .body(requestBody)
      .response(asJson[TokenCountResponse])

    request.send(backend).map { resp =>
      val result = handleResponse(resp, requestDescription)
      result.left.foreach(e => logger.error(s"[countTokens] ${e.message}")) // Log error if any
      result
    }
  }

  // Close the backend and shutdown the execution context if applicable
  def closeBackend(): Unit = {
    backend.close()
    ec match {
      case eces: ExecutionContextExecutorService =>
        eces.shutdown() // Shutdown if it's an ExecutionContextExecutorService
      case _ =>
        logger.debug("ExecutionContext is external; not shutting it down.") // Log if not shutting down
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
