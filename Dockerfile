# Use a JDK image
FROM eclipse-temurin:22-jdk-alpine

# Install bash for running commands
RUN apk add --no-cache bash

WORKDIR /usr/src/app

# Copy Maven wrapper & pom.xml first (for dependency caching)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy the source code
COPY src ./src

# Expose the port
EXPOSE 8080

# Run Spring Boot in dev mode
CMD ["./mvnw", "spring-boot:run", "-Dspring-boot.run.fork=false"]
