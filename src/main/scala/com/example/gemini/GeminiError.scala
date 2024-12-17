package com.example.gemini

// Define error codes as a sealed trait
sealed trait ErrorCode { def code: String }
object ErrorCode {
  case object HttpError extends ErrorCode { val code = "HTTP_ERROR" } // HTTP error code
  case object JsonError extends ErrorCode { val code = "JSON_DESERIALIZATION_ERROR" } // JSON deserialization error code
  case object Unexpected extends ErrorCode { val code = "UNEXPECTED_ERROR" } // Unexpected error code
}

// Define a sealed trait for Gemini errors
sealed trait GeminiError {
  def message: String // Error message
  def errorCode: ErrorCode // Associated error code
  def causeException: Option[Throwable] = None // Optional cause exception
  def toException: Exception = new RuntimeException(message, causeException.orNull) // Convert to exception
  def toMap: Map[String, Any] = Map(
    "errorCode" -> errorCode.code,
    "message" -> message
  ) ++ causeException.map("cause" -> _.getMessage) // Convert to map representation
}

// HTTP error with status code and response body
final case class HttpErrorStatus(code: Int, body: String, causeEx: Option[Throwable] = None) extends GeminiError {
  def message: String = s"HTTP error: $code, Response: $body"
  def errorCode: ErrorCode = ErrorCode.HttpError
  override def causeException: Option[Throwable] = causeEx
  override def toMap: Map[String, Any] =
    super.toMap ++ Map("httpStatusCode" -> code, "responseBody" -> body)
}

// JSON deserialization error with original JSON and cause
final case class JsonDeserializationError(original: String, cause: String, causeEx: Option[Throwable] = None) extends GeminiError {
  def message: String = s"Deserialization error: Original: $original, Cause: $cause"
  def errorCode: ErrorCode = ErrorCode.JsonError
  override def causeException: Option[Throwable] = causeEx
  override def toMap: Map[String, Any] =
    super.toMap ++ Map("originalJson" -> original, "deserializationCause" -> cause)
}

// Unexpected error with a cause
final case class UnexpectedError(cause: String, causeEx: Option[Throwable] = None) extends GeminiError {
  def message: String = s"Unexpected error: $cause"
  def errorCode: ErrorCode = ErrorCode.Unexpected
  override def causeException: Option[Throwable] = causeEx
  override def toMap: Map[String, Any] =
    super.toMap ++ Map("unexpectedCause" -> cause)
}
