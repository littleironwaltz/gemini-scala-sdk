package com.example.gemini

import sttp.client3._
import sttp.client3.circe._
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import io.circe.generic.auto._
import io.circe.Error
import scala.concurrent.{Future, ExecutionContext}
import com.typesafe.scalalogging.LazyLogging
import java.util.concurrent.Executors

/**
 * Asynchronous client for interacting with the Gemini API.
 * This object provides methods to communicate with Google's Gemini API,
 * handling model information, content generation, and token counting operations.
 */
object AsyncGeminiAPI extends LazyLogging {

  /**
   * Dedicated thread pool for handling HTTP requests.
   * Uses a fixed size of 4 threads for concurrent API calls.
   */
  private val httpExecutor = Executors.newFixedThreadPool(4)
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(httpExecutor)

  private val baseUrl = ConfigLoader.baseUrl
  private val backend = AsyncHttpClientFutureBackend()

  /**
   * Processes API responses and standardizes error handling.
   * Converts STTP response types into the application's error model.
   *
   * @param response The raw STTP response to process
   * @tparam T The expected success response type
   * @return Either a GeminiError for failures or the successful response of type T
   */
  private def handleResponse[T](response: Response[Either[ResponseException[String, Error], T]]): Either[GeminiError, T] = {
    response.body match {
      case Right(value) => Right(value)
      case Left(error) =>
        val statusCode = response.code.code
        error match {
          case HttpError(body, _) =>
            Left(HttpErrorStatus(statusCode, body))
          case DeserializationException(original, ex) =>
            Left(JsonDeserializationError(original, ex.getMessage))
        }
    }
  }

  /**
   * Fetches the list of available Gemini models.
   * Returns information about all models accessible with the provided API key.
   *
   * @param apiKey The authentication key for accessing the Gemini API
   * @return Future containing either an error or the list of available models
   */
  def getModels(apiKey: String): Future[Either[GeminiError, ModelList]] = {
    val request = basicRequest
      .get(uri"$baseUrl/models?key=$apiKey")
      .response(asJson[ModelList])

    request.send(backend).map { resp =>
      val result = handleResponse(resp)
      result.left.foreach(e => logger.error(s"[getModels] ${e.message}"))
      result
    }
  }

  /**
   * Retrieves detailed information about a specific Gemini model.
   * Handles both full model names (with "models/" prefix) and base names.
   *
   * @param modelName The name or ID of the model to query
   * @param apiKey The authentication key for accessing the Gemini API
   * @return Future containing either an error or detailed model information
   */
  def getModelDetails(modelName: String, apiKey: String): Future[Either[GeminiError, ModelInfo]] = {
    val pureModelName = modelName.stripPrefix("models/")
    val request = basicRequest
      .get(uri"$baseUrl/models/$pureModelName?key=$apiKey")
      .response(asJson[ModelInfo])

    request.send(backend).map { resp =>
      val result = handleResponse(resp)
      result.left.foreach(e => logger.error(s"[getModelDetails] ${e.message}"))
      result
    }
  }

  /**
   * Generates content using the specified Gemini model.
   * Sends a prompt to the model and receives generated content in response.
   *
   * @param modelName The name of the model to use for generation
   * @param prompt The input text to generate content from
   * @param config Optional configuration parameters for content generation
   * @param apiKey The authentication key for accessing the Gemini API
   * @return Future containing either an error or the generated content response
   */
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

    val request = basicRequest
      .post(uri"$baseUrl/models/$pureModelName:generateContent?key=$apiKey")
      .header("Content-Type", "application/json")
      .body(requestBody)
      .response(asJson[GenerateContentResponse])

    request.send(backend).map { resp =>
      val result = handleResponse(resp)
      result.left.foreach(e => logger.error(s"[generateContent] ${e.message}"))
      result
    }
  }

  /**
   * Calculates the token count for a given text using the specified model.
   * Useful for estimating costs and staying within model context limits.
   *
   * @param modelName The name of the model to use for token counting
   * @param text The input text to count tokens for
   * @param apiKey The authentication key for accessing the Gemini API
   * @return Future containing either an error or the token count response
   */
  def countTokens(
      modelName: String,
      text: String,
      apiKey: String
  ): Future[Either[GeminiError, TokenCountResponse]] = {
    val pureModelName = modelName.stripPrefix("models/")
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
      val result = handleResponse(resp)
      result.left.foreach(e => logger.error(s"[countTokens] ${e.message}"))
      result
    }
  }

  /**
   * Performs cleanup by shutting down the HTTP client and thread pool.
   * Should be called when the API client is no longer needed to prevent resource leaks.
   */
  def closeBackend(): Unit = {
    backend.close()
    httpExecutor.shutdown()
  }
}
