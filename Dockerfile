# Build stage
FROM eclipse-temurin:11-jdk AS build
WORKDIR /build

# Install sbt
RUN apt-get update && \
    apt-get install -y curl && \
    curl -L -o sbt-1.9.4.deb https://repo.scala-sbt.org/scalasbt/debian/sbt-1.9.4.deb && \
    dpkg -i sbt-1.9.4.deb && \
    apt-get update && \
    apt-get install -y sbt && \
    rm sbt-1.9.4.deb && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

COPY . .
RUN sbt assembly

# Run stage
FROM eclipse-temurin:11-jre-jammy
WORKDIR /app
COPY --from=build /build/target/scala-2.13/gemini-scala-sdk-assembly-*.jar /app/gemini-scala-sdk.jar
ENTRYPOINT ["java", "-jar", "/app/gemini-scala-sdk.jar"]
