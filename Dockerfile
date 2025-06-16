# Stage 1: Build using Gradle and JDK
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy all project files into the container
COPY . .

# Build the application using the Gradle wrapper
RUN ./gradlew installDist

# Stage 2: Run using lightweight JDK image
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy built app from previous stage
COPY --from=build /app/build/install/ktor-sample-sms /app

# Expose the port your Ktor app listens to
EXPOSE 8080

# Run the app
CMD ["./bin/ktor-sample-sms"]
