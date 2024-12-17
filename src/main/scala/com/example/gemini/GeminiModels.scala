// GeminiModels.scala
package com.example.gemini

import io.circe.generic.JsonCodec

// Configuration for text generation
@JsonCodec
case class GenerationConfig(
  temperature: Double = 0.7, // Controls randomness in generation
  topP: Double = 0.9, // Nucleus sampling parameter
  topK: Int = 40, // Top-K sampling parameter
  maxOutputTokens: Int = 1024 // Maximum number of tokens in output
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

// Part of the content
@JsonCodec
case class Part(text: String)

// Content consisting of multiple parts
@JsonCodec
case class Content(parts: Seq[Part])

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

// Candidate content with optional safety ratings and citation metadata
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

// Item of content with a role and parts
@JsonCodec
case class ContentItem(
  role: String, // Role of the content item (e.g., user, assistant)
  parts: Seq[Part] // Parts of the content
)

// Request to generate content
@JsonCodec
case class GenerateContentRequest(
  contents: Seq[ContentItem] // Contents to be used for generation
)

// Request to count tokens
@JsonCodec
case class CountTokensRequest(
  contents: Option[Seq[ContentItem]] = None, // Optional contents for token counting
  generateContentRequest: Option[GenerateContentRequest] = None // Optional content generation request
)

// Response for token count
@JsonCodec
case class TokenCountResponse(totalTokens: Int) // Total number of tokens counted
