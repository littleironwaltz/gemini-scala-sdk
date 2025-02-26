# Gemini API Scala SDK

A Scala-based asynchronous SDK for interacting with Google's Generative Language Gemini API. This SDK provides a simple and type-safe way to interact with Gemini's powerful language models.

## Features

- **Fully Asynchronous**: Built on STTP and Scala Futures for non-blocking operations.
- **Type-safe Models**: Comprehensive case classes and Circe codecs for all API models.
- **Structured Error Handling**: Provides detailed context for errors with structured types.
- **Configurable**: Supports environment variables and HOCON configuration files.
- **Production Ready**: Includes logging, proper resource management, and comprehensive tests.
- **Easy Integration**: Simple API methods to get models, generate content, and count tokens.

## Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Usage](#usage)
  - [Fetching Models](#fetching-models)
  - [Generating Content](#generating-content)
  - [Counting Tokens](#counting-tokens)
- [Error Handling](#error-handling)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgments](#acknowledgments)

## Installation

### SBT Dependency

Add the following dependency to your `build.sbt` file:

```scala
libraryDependencies += "com.example.gemini" %% "gemini-scala-sdk" % "1.0.0"
```

Ensure your `build.sbt` includes the necessary resolver and Scala version:

```scala
scalaVersion := "2.13.12"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
```

## Quick Start

### Clone the Repository

```bash
git clone https://github.com/yourusername/gemini-scala-sdk.git
cd gemini-scala-sdk
```

### Set Your API Key

The SDK requires a Gemini API key. You can set it via environment variables or configuration files.

- **Environment Variable**:

  ```bash
  export APP_GEMINI_API_KEY="your-api-key"
  ```

- **Configuration File** (`src/main/resources/application.conf`):

  ```hocon
  app.gemini.apiKey = "your-api-key"
  ```

### Build and Run Tests

Build the project and run the test suite to ensure everything is set up correctly.

```bash
sbt clean compile test
```

## Configuration

The SDK can be configured using environment variables or a HOCON configuration file (`application.conf`). The configuration allows you to set parameters like API key, base URL, thread pool size, and request timeout.

### `application.conf`

```hocon
app {
  gemini {
    apiKey = ${?APP_GEMINI_API_KEY}        # Gemini API Key
    baseUrl = ${?APP_GEMINI_BASEURL}       # Base URL for the API
    threadPoolSize = ${?APP_GEMINI_THREADPOOL_SIZE}       # Thread pool size for async operations
    requestTimeoutMillis = ${?APP_GEMINI_TIMEOUT_MS}      # Timeout for API requests
  }
}

logger {
  level = ${?LOGGER_LEVEL}                 # Logging level (e.g., INFO, DEBUG)
}
```

### Environment Variables

- `APP_GEMINI_API_KEY`: Your Gemini API key.
- `APP_GEMINI_BASEURL`: Base URL for the Gemini API.
- `APP_GEMINI_THREADPOOL_SIZE`: Number of threads for the executor service.
- `APP_GEMINI_TIMEOUT_MS`: Timeout in milliseconds for API requests.
- `LOGGER_LEVEL`: Logging level for the application.

## Usage

### Import the SDK

```scala
import com.example.gemini._
```

### Initialize the API Client

```scala
import scala.concurrent.ExecutionContext.Implicits.global

val api = AsyncGeminiAPI.default
val apiKey = sys.env.getOrElse("APP_GEMINI_API_KEY", "your-api-key")
```

### Fetching Models

To retrieve the list of available models:

```scala
api.getModels(apiKey).map {
  case Right(modelList) =>
    modelList.models.foreach { model =>
      println(s"Model Name: ${model.name}, Display Name: ${model.displayName}")
    }
  case Left(error) =>
    println(s"Error fetching models: ${error.message}")
}
```

### Generating Content

Generate content using a specific model:

```scala
val modelName = "models/gemini-2.0-flash-exp"
val prompt = "What is the capital of France?"

api.generateContent(modelName, prompt, config = None, apiKey).map {
  case Right(response) =>
    val generatedText = response.candidates.headOption
      .flatMap(_.content.parts.headOption)
      .map(_.text)
      .getOrElse("No content generated.")
    println(s"Generated Text: $generatedText")
  case Left(error) =>
    println(s"Error generating content: ${error.message}")
}
```

### Counting Tokens

Count tokens for a given text input:

```scala
val textToCount = "Hello, how are you?"

api.countTokens(modelName, textToCount, apiKey).map {
  case Right(tokenCountResponse) =>
    println(s"Total Tokens: ${tokenCountResponse.totalTokens}")
  case Left(error) =>
    println(s"Error counting tokens: ${error.message}")
}
```

## Error Handling

The SDK provides structured error types to help you handle exceptions and errors gracefully.

```scala
sealed trait GeminiError {
  def message: String
  def errorCode: ErrorCode
}

case class HttpErrorStatus(code: Int, body: String) extends GeminiError
case class JsonDeserializationError(original: String, cause: String) extends GeminiError
case class UnexpectedError(cause: String) extends GeminiError
```

Example of handling errors:

```scala
api.getModels(apiKey).map {
  case Right(modelList) =>
    // Process models
  case Left(error: HttpErrorStatus) =>
    println(s"HTTP Error: ${error.code}, Body: ${error.body}")
  case Left(error: JsonDeserializationError) =>
    println(s"Deserialization Error: ${error.cause}")
  case Left(error) =>
    println(s"An unexpected error occurred: ${error.message}")
}
```

## Testing

The project includes a comprehensive set of tests using ScalaTest. Tests cover successful API interactions as well as error handling scenarios. Mocked HTTP responses are used to simulate API responses.

### Running Tests

```bash
sbt test
```

### Sample Test Specification

```scala
class AsyncGeminiAPISpec extends AsyncWordSpec with Matchers {
  "AsyncGeminiAPI" should {
    "retrieve models successfully" in {
      // Test implementation
    }
    // Additional test cases
  }
}
```

## Project Structure

```
gemini-scala-sdk/
├── build.sbt
├── src/
│   ├── main/
│   │   ├── resources/
│   │   │   ├── application.conf
│   │   │   └── logback.xml
│   │   └── scala/
│   │       └── com/
│   │           └── example/
│   │               └── gemini/
│   │                   ├── AsyncGeminiAPI.scala
│   │                   ├── ConfigLoader.scala
│   │                   ├── GeminiError.scala
│   │                   ├── GeminiModels.scala
│   │                   └── TestAsyncGeminiSDK.scala
│   └── test/
│       └── scala/
│           └── com/
│               └── example/
│                   └── gemini/
│                       └── AsyncGeminiAPISpec.scala
```

## Contributing

We welcome contributions to the project! To contribute:

1. Fork the repository on GitHub.
2. Create a feature branch:

   ```bash
   git checkout -b feature/your-feature-name
   ```

3. Commit your changes:

   ```bash
   git commit -am 'Add your feature'
   ```

4. Push to your branch:

   ```bash
   git push origin feature/your-feature-name
   ```

5. Open a Pull Request on GitHub.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Deployment

### Maven Central
The SDK is published to Maven Central. To publish a new version:

1. Set version tag:
```bash
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
```

2. The CI/CD pipeline will automatically:
- Run tests
- Build the package
- Publish to Maven Central

### Docker
Build and run the Docker image:

```bash
docker-compose build
docker-compose up
```

Environment variables can be set in a `.env` file:
```env
APP_GEMINI_API_KEY=your_api_key
APP_GEMINI_BASEURL=custom_url
APP_GEMINI_TIMEOUT_MS=30000
LOGGER_LEVEL=INFO
```

## Acknowledgments

- Built with [STTP](https://sttp.softwaremill.com/en/latest/) for HTTP communication.
- JSON parsing and encoding with [Circe](https://circe.github.io/circe/).
- Configuration management using [Typesafe Config](https://github.com/lightbend/config).
- Logging with [Scala Logging](https://github.com/lightbend/scala-logging) and [Logback](http://logback.qos.ch/).

---

*Note: This SDK is not an official Google product and is maintained independently.*
