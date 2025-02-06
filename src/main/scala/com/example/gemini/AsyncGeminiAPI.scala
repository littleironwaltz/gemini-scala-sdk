package com.example.gemini

import sttp.client3._
import sttp.client3.circe._
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import io.circe.{Encoder, Decoder, Error}
import io.circe.generic.auto._
import io.circe.syntax._
import scala.concurrent.{Future, ExecutionContext, ExecutionContextExecutorService}
import com.example.gemini.logging.GeminiLogger
import java.util.concurrent.Executors
import sttp.model.Uri
import sttp.client3.circe.asJson

class AsyncGeminiAPI(
    implicit val ec: ExecutionContext,
    backend: SttpBackend[Future, Any]
) extends GeminiLogger {

  type GeminiResult[T] = Future[Either[GeminiError, T]]
  type GeminiResponse[T] = Response[Either[ResponseException[String, Error], T]]

  private val baseUrl = ConfigLoader.baseUrl

  private case class RequestConfig(path: String, apiKey: String) {
    def buildUri = uri"$baseUrl/$path?key=$apiKey"
  }

  private def normalizeModelName(modelName: String): String = {
    val prefix = "models/"
    if (modelName.startsWith(prefix)) modelName.stripPrefix(prefix)
    else modelName
  }

  private def executeRequest[T: Decoder, B: Encoder](
      config: RequestConfig,
      method: String,
      body: Option[B] = None
  ): GeminiResult[T] = {
    val baseRequest = basicRequest.response(asJson[T])
    
    val request = (method, body) match {
      case ("GET", _) => baseRequest.get(config.buildUri)
      case ("POST", Some(data)) => baseRequest
        .post(config.buildUri)
        .header("Content-Type", "application/json")
        .body(data.asJson)
      case ("POST", None) => baseRequest.post(config.buildUri)
      case (unsupported, _) => throw new IllegalArgumentException(s"Unsupported HTTP method: $unsupported")
    }

    handleRequest(request, s"$method ${config.path}")
  }

  private def handleRequest[T](request: RequestT[Identity, Either[ResponseException[String, Error], T], Any], context: String): GeminiResult[T] = {
    val Array(method, path) = context.split(" ")
    logRequest(method, path)
    request.send(backend).map { resp =>
      val result = handleResponse(resp, context)
      result.left.foreach(e => logError(context, e))
      result
    }
  }

  // Handle HTTP response and map to either GeminiError or the expected type
  private def handleResponse[T](
      response: Response[Either[ResponseException[String, Error], T]],
      requestDescription: String
  ): Either[GeminiError, T] = {
    response.body match {
      case Right(value) => Right(value)
      case Left(error) =>
        val statusCode = response.code.code
        val detailedContext = s"Request: $requestDescription, StatusCode: $statusCode"
        error match {
          case HttpError(body, _) =>
            Left(HttpErrorStatus(statusCode, s"$detailedContext, Body: $body"))
          case DeserializationException(original, ex) =>
            Left(JsonDeserializationError(original, s"$detailedContext, Cause: ${ex.getMessage}"))
        }
    }
  }

  // Fetch list of models
  /**
   * Retrieves a list of available Gemini models.
   * Use this to discover supported models and their capabilities.
   *
   * @param apiKey API key for authentication
   * @return Future containing either a GeminiError or list of available models
   */
  def getModels(apiKey: String): GeminiResult[ModelList] = {
    executeRequest[ModelList, Unit](
      RequestConfig("models", apiKey),
      "GET"
    )
  }

  /**
   * Fetches detailed information about a specific Gemini model.
   * Provides model capabilities, limits, and supported features.
   *
   * @param modelName Name of the model (with or without 'models/' prefix)
   * @param apiKey API key for authentication
   * @return Future containing either a GeminiError or detailed model information
   */
  def getModelDetails(modelName: String, apiKey: String): GeminiResult[ModelInfo] = {
    executeRequest[ModelInfo, Unit](
      RequestConfig(s"models/${normalizeModelName(modelName)}", apiKey),
      "GET"
    )
  }

  /**
   * Generates content using the specified Gemini model.
   * Supports text generation with optional configuration for controlling output characteristics.
   *
   * @param modelName Name of the model to use (with or without 'models/' prefix)
   * @param prompt Text prompt for content generation
   * @param config Optional configuration for controlling temperature, tokens, etc.
   * @param apiKey API key for authentication
   * @return Future containing either a GeminiError or generated content response
   */
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
    executeRequest[GenerateContentResponse, GenerateContentRequest](
      RequestConfig(s"models/$name:generateContent", apiKey),
      "POST",
      Some(requestBody)
    )
  }

  /**
   * Counts tokens in the provided text using the specified model's tokenizer.
   * Useful for estimating costs and staying within model input limits.
   *
   * @param modelName Name of the model whose tokenizer to use
   * @param text Text to analyze for token count
   * @param apiKey API key for authentication
   * @return Future containing either a GeminiError or token count response
   */
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
    executeRequest[TokenCountResponse, CountTokensRequest](
      RequestConfig(s"models/$name:countTokens", apiKey),
      "POST",
      Some(requestBody)
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
