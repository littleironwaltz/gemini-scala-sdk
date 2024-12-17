package com.example.gemini

/**
 * Base trait for all Gemini API error types.
 * Provides a unified error handling interface for the API client.
 * All specific error types must implement the message method to provide error details.
 */
sealed trait GeminiError {
  /**
   * Returns a human-readable error message describing the error condition.
   * @return String containing the error message
   */
  def message: String
}

/**
 * Represents HTTP-level errors returned by the Gemini API.
 * 
 * @param code The HTTP status code returned by the API
 * @param body The response body containing error details
 */
final case class HttpErrorStatus(code: Int, body: String) extends GeminiError {
  def message: String = s"HTTP error: $code, Response: $body"
}

/**
 * Represents errors that occur during JSON deserialization of API responses.
 * 
 * @param original The original JSON string that failed to deserialize
 * @param cause The error message describing why deserialization failed
 */
final case class JsonDeserializationError(original: String, cause: String) extends GeminiError {
  def message: String = s"Deserialization error: Original: $original, Cause: $cause"
}

/**
 * Represents unexpected errors that don't fall into other categories.
 * Used for general error conditions not covered by other error types.
 * 
 * @param cause Description of the unexpected error condition
 */
final case class UnexpectedError(cause: String) extends GeminiError {
  def message: String = s"Unexpected error: $cause"
}
