# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy root pom and modules
COPY pom.xml .
COPY auradb-common ./auradb-common
COPY auradb-control-plane ./auradb-control-plane
COPY auradb-route-service ./auradb-route-service

# Build only the route service and its dependencies (common)
RUN mvn clean package -pl auradb-route-service -am -DskipTests

# Stage 2: Create the minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/auradb-route-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
