/*
 * Asynchronous Gemini API Client Implementation
 * 
 * This module provides a non-blocking client for interacting with Google's Gemini API.
 * Key responsibilities:
 * - HTTP request generation and execution
 * - Error handling and response processing
 * - Resource management (connection pooling, thread management)
 * - Type-safe API interactions
 * 
 * The implementation uses STTP for HTTP communication and provides
 * Future-based asynchronous operations with proper resource cleanup.
 */

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

class AsyncGeminiAPI(
    implicit val ec: ExecutionContext,
    backend: SttpBackend[Future, Any]
) extends GeminiLogger {

  type GeminiResult[T] = Future[Either[GeminiError, T]]
  type GeminiResponse[T] = Response[Either[ResponseException[String, Error], T]]

  private val baseUrl = uri"https://generativelanguage.googleapis.com/v1"

  /**
   * Configuration for building API requests.
   * Encapsulates path and authentication details.
   * 
   * @param path API endpoint path
   * @param apiKey Authentication key
   */
  private case class RequestConfig(path: String, apiKey: String) {
    def buildUri = {
      val (basePath, method) = path.split(":") match {
        case Array(base, method) => (base, Some(method))
        case Array(base) => (base, None)
      }
      
      val segments = basePath match {
        case "" => List("models")
        case p if p.startsWith("/") => p.substring(1).split("/").toList
        case p => p.split("/").toList
      }

      val uri = method match {
        case Some(m) => baseUrl.addPath(segments).addPath(s":$m")
        case None => baseUrl.addPath(segments)
      }

      uri.addParam("key", apiKey)
    }
  }

  /**
   * Normalizes model names by ensuring 'models/' prefix.
   * Ensures consistent model name format for API requests.
   * 
   * @param modelName Raw model name (with or without prefix)
   * @return Normalized model name with models/ prefix
   */
  private def normalizeModelName(modelName: String): String = {
    if (modelName.startsWith("models/")) modelName else s"models/$modelName"
  }
  /**
   * Executes an HTTP request with proper error handling and logging.
   *
   * @param config Request configuration containing path and API key
   * @param method HTTP method (GET/POST)
   * @param body Optional request body for POST requests
   * @tparam T Expected response type with Circe decoder
   * @tparam B Request body type with Circe encoder
   * @return Future containing either a GeminiError or successful response
   */
  /**
   * Builds an HTTP request with common configuration.
   *
   * @param method HTTP method (GET/POST)
   * @param path API endpoint path
   * @param body Optional request body for POST requests
   * @param apiKey API authentication key
   * @return Configured HTTP request
   */
  private def buildRequest[T: Decoder, B: Encoder](
      method: String,
      path: String,
      body: Option[B],
      apiKey: String
  ): RequestT[Identity, Either[ResponseException[String, Error], T], Any] = {
    val config = RequestConfig(path, apiKey)
    val request = method.toUpperCase match {
      case "GET" =>
        basicRequest.get(config.buildUri)
      case "POST" =>
        basicRequest.post(config.buildUri).header("Content-Type", "application/json")
      case _ => throw new IllegalArgumentException(s"Unsupported HTTP method: $method")
    }
    
    val baseRequest = request.response(asJson[T])
    body match {
      case Some(b) => baseRequest.body(b.asJson)
      case None => baseRequest
    }
  }

  private def executeRequest[T: Decoder, B: Encoder](
      config: RequestConfig,
      method: String,
      body: Option[B] = None
  ): GeminiResult[T] = {
    val request = buildRequest[T, B](method, config.path, body, config.apiKey)
    handleRequest(request, s"$method ${config.path}")
  }

  /**
   * Handles request execution with logging and error processing.
   * Logs request details before execution and maps errors appropriately.
   *
   * @param request STTP request to execute
   * @param context Request context for logging (e.g., "GET /models")
   * @tparam T Expected response type
   * @return Future containing either a GeminiError or successful response
   */
  private def handleRequest[T](request: RequestT[Identity, Either[ResponseException[String, Error], T], Any], context: String): GeminiResult[T] = {
    val parts = context.split(" ")
    val method = parts(0)
    val path = if (parts.length > 1) parts(1) else ""
    logRequest(method, path)
    request.send(backend).map { resp =>
      val result = handleResponse(resp, context)
      result.left.foreach(e => logError(context, e))
      result
    }
  }

  // Handle HTTP response and map to either GeminiError or the expected type
  /**
   * Processes HTTP response and maps errors to domain-specific types.
   * 
   * Success case (Right):
   * - Returns the successfully decoded response
   * 
   * Error cases (Left):
   * - HTTP errors (e.g., 404: "Model not found", 401: "Invalid API key")
   * - JSON deserialization errors (e.g., unexpected response format)
   * 
   * @param response Raw HTTP response from STTP
   * @param requestDescription Context for error messages
   * @return Either a GeminiError or successful response
   */
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
   * @param apiKey API authentication key
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
   * @param apiKey API authentication key
   * @return Future containing either a GeminiError or detailed model information
   */
  def getModelDetails(modelName: String, apiKey: String): GeminiResult[ModelInfo] = {
    val modelPath = normalizeModelName(modelName)
    executeRequest[ModelInfo, Unit](
      RequestConfig(modelPath, apiKey),
      "GET"
    )
  }

  /**
   * Generates content using the specified Gemini model.
   * 
   * Handles model name normalization and request construction for content generation.
   * The prompt is wrapped in a ContentItem with "user" role for proper API formatting.
   * 
   * @param modelName Name of the model to use (with or without 'models/' prefix)
   * @param prompt Text prompt for content generation
   * @param config Optional generation parameters (temperature, tokens, etc.)
   * @param apiKey API authentication key
   * @return Future containing either a GeminiError or generated content response
   */
  def generateContent(
      modelName: String,
      prompt: String,
      config: Option[GenerationConfig],
      apiKey: String
  ): GeminiResult[GenerateContentResponse] = {
    val modelPath = normalizeModelName(modelName)
    val requestBody = GenerateContentRequest(
      model = modelPath,
      contents = Seq(ContentItem("user", Seq(Part(prompt))))
    )
    executeRequest[GenerateContentResponse, GenerateContentRequest](
      RequestConfig(s"$modelPath:generateContent", apiKey),
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
    val modelPath = normalizeModelName(modelName)
    val requestBody = CountTokensRequest(
      model = modelPath,
      contents = Some(Seq(ContentItem("user", Seq(Part(text))))),
      generateContentRequest = None
    )
    executeRequest[TokenCountResponse, CountTokensRequest](
      RequestConfig(s"$modelPath:countTokens", apiKey),
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
