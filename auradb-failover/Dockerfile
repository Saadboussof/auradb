# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy entire source tree
COPY . .

# Build only the failover module and its dependencies
RUN mvn clean package -pl auradb-failover -am -DskipTests

# Stage 2: Create the minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/auradb-failover/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
