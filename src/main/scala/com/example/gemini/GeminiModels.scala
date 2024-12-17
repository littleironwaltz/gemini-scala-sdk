package com.example.gemini

import io.circe.generic.JsonCodec

/**
 * Configuration parameters for content generation.
 * Controls various aspects of the model's generation behavior.
 *
 * @param temperature Controls randomness in the output. Range: [0.0, 1.0]
 *                    Higher values produce more creative but less focused output
 *                    Lower values produce more deterministic and focused output
 * @param topP        Nucleus sampling parameter. Range: [0.0, 1.0]
 *                    Controls diversity by considering only the most likely tokens
 * @param topK        Limits the number of tokens considered for each generation step
 *                    Higher values increase diversity but may reduce quality
 * @param maxOutputTokens Maximum length of generated content in tokens
 */
@JsonCodec
case class GenerationConfig(
  temperature: Double = 0.7,
  topP: Double = 0.9,
  topK: Int = 40,
  maxOutputTokens: Int = 1024
)

/**
 * Represents a single message in a conversation with the model.
 * Used for structuring inputs in chat-based interactions.
 *
 * @param role    Identifies the message sender (e.g., "user", "assistant", "system")
 * @param content The actual message text
 */
@JsonCodec
case class ChatMessage(role: String, content: String)

/**
 * Detailed information about a specific Gemini model.
 * Contains model capabilities and limitations.
 *
 * @param name The unique identifier for the model
 * @param displayName Human-readable name for the model
 * @param description Detailed description of the model's capabilities
 * @param inputTokenLimit Maximum number of tokens allowed in input
 * @param outputTokenLimit Maximum number of tokens the model can generate
 */
@JsonCodec
case class ModelInfo(
  name: String,
  displayName: String,
  description: String,
  inputTokenLimit: Int,
  outputTokenLimit: Int
)

/**
 * Container for a list of available models.
 * Used in the response of the models listing endpoint.
 *
 * @param models Sequence of available model information
 */
@JsonCodec
case class ModelList(models: Seq[ModelInfo])

/**
 * Represents a text segment in the model's input or output.
 *
 * @param text The actual text content
 */
@JsonCodec
case class Part(text: String)

/**
 * Container for multiple content parts.
 * Allows for structured content representation.
 *
 * @param parts Sequence of content parts
 */
@JsonCodec
case class Content(parts: Seq[Part])

/**
 * Safety evaluation for generated content.
 * Indicates potential concerns in the content.
 *
 * @param category The type of safety concern (e.g., "harassment", "hate_speech")
 * @param probability The likelihood of the content falling into this category
 */
@JsonCodec
case class SafetyRating(
  category: String,
  probability: String
)

/**
 * Reference information for source material.
 *
 * @param url The source URL
 * @param title The title of the referenced content
 */
@JsonCodec
case class Citation(
  url: String,
  title: String
)

/**
 * Container for multiple citations.
 *
 * @param citations Sequence of citation information
 */
@JsonCodec
case class CitationMetadata(
  citations: Seq[Citation]
)

/**
 * Represents a single generated response from the model.
 * Contains the generated content and associated metadata.
 *
 * @param content The generated content
 * @param safetyRatings Optional safety evaluations for the content
 * @param citationMetadata Optional references to source materials
 */
@JsonCodec
case class Candidate(
  content: Content,
  safetyRatings: Option[Seq[SafetyRating]] = None,
  citationMetadata: Option[CitationMetadata] = None
)

/**
 * Response from the content generation API.
 * May contain multiple candidates if the model was configured to generate alternatives.
 *
 * @param candidates Sequence of generated responses
 */
@JsonCodec
case class GenerateContentResponse(
  candidates: Seq[Candidate]
)

/**
 * Represents a single content element in a conversation or prompt.
 * Used for structuring input to the model.
 *
 * @param role Identifies the source of the content (e.g., "user", "assistant")
 * @param parts The actual content broken into parts
 */
@JsonCodec
case class ContentItem(
  role: String,
  parts: Seq[Part]
)

/**
 * Request structure for content generation.
 * Contains the input content items for the model.
 *
 * @param contents Sequence of content items forming the input
 */
@JsonCodec
case class GenerateContentRequest(
  contents: Seq[ContentItem]
)

/**
 * Request structure for token counting.
 * Can accept either direct content or a full generation request.
 *
 * @param contents Optional sequence of content items to count tokens for
 * @param generateContentRequest Optional full generation request to count tokens for
 */
@JsonCodec
case class CountTokensRequest(
  contents: Option[Seq[ContentItem]] = None,
  generateContentRequest: Option[GenerateContentRequest] = None
)

/**
 * Response from the token counting endpoint.
 * Provides the total number of tokens in the input.
 *
 * @param totalTokens The total number of tokens counted
 */
@JsonCodec
case class TokenCountResponse(totalTokens: Int)
