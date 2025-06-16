# Stage 1: Build the fat JAR using Gradle
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy project files into container
COPY . .

# Build the shadowJar (fat jar)
RUN ./gradlew shadowJar

# Stage 2: Run the app using slim JDK
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=build /app/build/libs/ktor-app.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
