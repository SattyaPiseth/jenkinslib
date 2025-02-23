# First stage: Build the application
FROM gradle:7.5.1-jdk17 AS build

# Set the working directory
WORKDIR /app

# Copy Gradle wrapper and dependency files first (to leverage Docker cache)
COPY gradle /app/gradle
COPY gradlew /app/gradlew
COPY build.gradle /app/build.gradle
COPY settings.gradle /app/settings.gradle

# Set execute permission for the Gradle wrapper
RUN chmod +x gradlew

# Download dependencies (to leverage Docker cache)
RUN ./gradlew build -x test --no-daemon || return 0

# Copy the source code
COPY src /app/src

# Package the application
RUN ./gradlew bootJar -x test --no-daemon

# Second stage: Run the application
FROM openjdk:17-jdk-alpine

# Set the working directory
WORKDIR /app

# Copy the packaged JAR file from the build stage
COPY --from=build /app/build/libs/*.jar /app/

# Expose the port the application runs on
EXPOSE 8080

# Set the entrypoint to run the application with dynamic JAR file name
ENTRYPOINT ["sh", "-c", "java -jar -Dspring.profiles.active=dev /app/$(ls /app | grep .jar)"]
