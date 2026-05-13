# syntax=docker/dockerfile:1.6

# ===== Build stage =====
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /build

# Copy Maven wrapper and pom first so we can cache the dependency layer
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Copy sources and build the runnable jar
COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# ===== Runtime stage =====
FROM eclipse-temurin:17-jre

# Useful JVM defaults for a memory-constrained container
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"

WORKDIR /app
COPY --from=builder /build/target/altforge-backend-*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
