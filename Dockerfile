# Multi-stage build for optimal image size
FROM maven:3.8.6-eclipse-temurin-17 AS builder
# Set working directory
WORKDIR /app
# Copy pom.xml first for better layer caching
COPY pom.xml .
# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B
# Copy source code
COPY src ./src
# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
# Create non-root user for security
RUN groupadd -r -g 1001 appgroup && \
    useradd -r -u 1001 -g appgroup appuser
WORKDIR /app
# Copy JAR file from builder stage
COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar
# Switch to non-root user
USER appuser:appgroup
# Expose port
EXPOSE 8080
# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/v1/accounts/health || exit 1
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]