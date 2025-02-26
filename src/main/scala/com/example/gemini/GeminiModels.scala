/*
 * Data Models for Gemini API
 * 
 * Defines the core data structures used in the Gemini API:
 * - Request/Response models
 * - Configuration types
 * - Content representation
 * - Safety and citation metadata
 * 
 * Features:
 * - Circe JSON codec integration
 * - Type-safe model hierarchies
 * - Shared traits for common patterns
 * - Comprehensive documentation
 */

package com.example.gemini

import io.circe.generic.JsonCodec

/**
 * Configuration for controlling the text generation behavior.
 * - temperature: Higher values (0.7-1.0) make output more creative but less focused
 * - topP: Controls diversity via nucleus sampling (0.9 recommended for natural language)
 * - topK: Limits vocabulary diversity to top K tokens (40 is balanced default)
 * - maxOutputTokens: Hard limit on response length for safety and resource management
 */
@JsonCodec
case class GenerationConfig(
  temperature: Double = 0.7,
  topP: Double = 0.9,
  topK: Int = 40,
  maxOutputTokens: Int = 1024
)

// Represents a chat message
@JsonCodec
case class ChatMessage(role: String, content: String)

// Information about a model
@JsonCodec
case class ModelInfo(
  name: String, // Model's name
  displayName: String, // Display name for the model
  description: String, // Description of the model
  inputTokenLimit: Int, // Maximum input tokens allowed
  outputTokenLimit: Int // Maximum output tokens allowed
)

// List of models
@JsonCodec
case class ModelList(models: Seq[ModelInfo])

/**
 * Common trait for types containing text parts.
 * Used by Content and ContentItem to share functionality.
 */
private[gemini] trait HasParts {
  def parts: Seq[Part]
}

@JsonCodec
case class Part(text: String)

@JsonCodec
case class Content(parts: Seq[Part]) extends HasParts

// Safety rating for content
@JsonCodec
case class SafetyRating(
  category: String, // Category of safety concern
  probability: String // Probability of the concern
)

// Citation information
@JsonCodec
case class Citation(
  url: String, // URL of the citation
  title: String // Title of the citation
)

// Metadata for citations
@JsonCodec
case class CitationMetadata(
  citations: Seq[Citation] // List of citations
)

/**
 * Represents a generated content candidate with optional safety ratings and citations.
 * Safety ratings are kept as Option[Seq] since their absence has semantic meaning
 * different from an empty list (None indicates no rating performed).
 */
@JsonCodec
case class Candidate(
  content: Content,
  safetyRatings: Option[Seq[SafetyRating]] = None,
  citationMetadata: Option[CitationMetadata] = None
)

// Response for content generation
@JsonCodec
case class GenerateContentResponse(
  candidates: Seq[Candidate] // List of candidate contents
)

/**
 * Represents a content item with a specific role in the conversation.
 * Used in both content generation and token counting requests.
 */
@JsonCodec
case class ContentItem(
  role: String,
  parts: Seq[Part]
) extends HasParts

// Request to generate content
@JsonCodec
case class GenerateContentRequest(
  model: String, // Model to use for generation
  contents: Seq[ContentItem] // Contents to be used for generation
)

/**
 * Request for counting tokens in content.
 * Contents is kept as Option[Seq] since None has different semantic meaning
 * from an empty sequence in the API contract.
 */
@JsonCodec
case class CountTokensRequest(
  model: String, // Model to use for token counting
  contents: Option[Seq[ContentItem]] = None,
  generateContentRequest: Option[GenerateContentRequest] = None
)

// Response for token count
@JsonCodec
case class TokenCountResponse(totalTokens: Int) // Total number of tokens counted
