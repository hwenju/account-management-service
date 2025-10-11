# Multi-stage build for optimal image size
FROM maven:3.8.6-openjdk-17 AS builder
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
FROM openjdk:17-jre-slim
# Create non-root user for security
RUN addgroup --system --gid 1000 appgroup && \
    adduser --system --uid 1000 --group appuser
WORKDIR /app
# Copy JAR file from builder stage
COPY --from=builder /app/target/*.jar app.jar
# Change ownership to non-root user
RUN chown appuser:appgroup app.jar
# Switch to non-root user
USER appuser:appgroup
# Expose port
EXPOSE 8080
# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/v1/accounts/health || exit 1
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]