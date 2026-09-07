# ==========================================
# Stage 1: Build & Package (Maven + JDK 21 Alpine)
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper & POM to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline -B

# Copy source code and build production JAR
COPY src ./src
RUN ./mvnw -q clean package -DskipTests -B

# ==========================================
# Stage 2: Minimal Production Runtime (JRE 21 Alpine)
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Create non-root user and group for security best practices
RUN addgroup -S spring && adduser -S spring -G spring

# Copy compiled JAR from builder stage
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Switch to non-root user
USER spring:spring

# Expose backend application port
EXPOSE 8080

# Configure container-aware JVM memory options
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]