# Gemini API Scala SDK

A Scala-based, asynchronous SDK for interacting with Google's Generative Language Gemini API. This SDK provides a simple and type-safe way to:

- Retrieve a list of available models
- Fetch detailed information about a specific model
- Generate content based on user prompts
- Count tokens in a given text

All requests are handled asynchronously using [STTP](https://sttp.softwaremill.com/) and Circe for JSON serialization/deserialization.

## Features

- **Asynchronous I/O**: Non-blocking HTTP calls using `sttp` and `AsyncHttpClientFutureBackend`.
- **Type-safe Models**: Automatically derived JSON models with Circe.
- **Error Handling**: Distinct error types (`GeminiError`) for HTTP and JSON parsing errors.
- **Easy Configuration**: Load your API key and other settings from `application.conf`.
- **Logging**: Configurable logging via Logback and Scala Logging.
- **Testable**: Comes with basic ScalaTest specs and a sample test class. Easily integrated into CI pipelines.

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [Testing](#testing)
- [Contributing](#contributing)
- [License](#license)

## Requirements

- Scala 2.13 or later
- sbt 1.0 or later
- A valid Gemini API key from Google (see [Google Generative AI Developer Guide](https://cloud.google.com/generative-ai/) for details on obtaining an API key)

## Installation

Clone this repository:

```bash
git clone https://github.com/your-username/gemini-scala-sdk.git
cd gemini-scala-sdk
```

Build the project:

```bash
sbt update
sbt compile
```

## Configuration

Place your Gemini API key in `src/main/resources/application.conf`:

```hocon
app {
  gemini {
    apiKey = "YOUR_API_KEY"
    baseUrl = "https://generativelanguage.googleapis.com/v1beta"
  }
}
```

**Note:** For security and best practices, it’s recommended to use environment variables or a secret manager in production environments rather than committing your API key to the repository.

You can also customize logging by modifying `src/main/resources/logback.xml`.

## Usage

### Running the Sample Script

`TestAsyncGeminiSDK.scala` demonstrates basic usage:

```bash
sbt "runMain com.example.gemini.TestAsyncGeminiSDK"
```

This will:

1. Fetch available models
2. Fetch details of a specific model
3. Generate content for a given prompt
4. Count tokens for a given text

The output will appear on the console, formatted by the logging configuration.

### Integrating into Your Code

You can integrate the SDK by importing the relevant classes and invoking methods from `AsyncGeminiAPI`:

```scala
import com.example.gemini._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

val apiKey = ConfigLoader.apiKey // or load from env variable
val modelName = "models/gemini-2.0-flash-exp"

// Example: Get models
val modelsFuture: Future[Either[GeminiError, ModelList]] = AsyncGeminiAPI.getModels(apiKey)
modelsFuture.map {
  case Right(modelList) =>
    println("Available models:")
    modelList.models.foreach(m => println(m.name))
  case Left(error) =>
    println(s"Failed to fetch models: ${error.message}")
}
```

## Testing

The SDK includes a basic test suite (`AsyncGeminiAPISpec.scala`) using ScalaTest:

```bash
sbt test
```

**Note:** The existing tests make real API calls. For automated CI testing, consider using mock servers or integration tests that run with controlled responses. You will need a valid API key and network access to pass these tests.

## Contributing

Contributions are welcome! Feel free to:

- Fork this repository
- Create a feature branch (`git checkout -b feature/my-new-feature`)
- Commit your changes (`git commit -am 'Add new feature'`)
- Push to the branch (`git push origin feature/my-new-feature`)
- Open a Pull Request

Before submitting a PR, please ensure:
- Code is formatted consistently (`scalafmt`/`scalastyle` recommended).
- Tests pass locally.
- Documentation is updated if needed.

## License

This project is licensed under the [MIT License](LICENSE). Feel free to use, modify, and distribute as allowed by the license.
