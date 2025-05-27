# Stage 1: Build Spring Boot App
FROM maven:latest as spring-builder

# Set the working directory for the build context
WORKDIR /app

# Copy the entire project into the build context
COPY pom.xml .

# Copy the source code to the working directory
COPY src ./src

# Clean and build
RUN mvn clean package -am

# Debug: List the contents of the target directory to verify the JAR file location
RUN ls -la target/

# Stage 2: Serve Spring Boot App
FROM eclipse-temurin:21-jdk

# Set the working directory in the final image
WORKDIR /app

# Copy built Spring Boot JAR from previous stage
COPY --from=spring-builder /app/target/botcommanderbackend.jar /app/javaMicroService.jar

# Expose the port the app runs on
EXPOSE 8080

# Command to run the Spring Boot app
CMD ["java", "-jar", "/app/javaMicroService.jar"]
