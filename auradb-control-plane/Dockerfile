FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy entire source tree (respecting .dockerignore)
COPY . .

# Build the common library and control-plane jar
# -pl specifies the project/module to build
# -am means "also make dependencies" (like auradb-common)
RUN mvn clean package -pl auradb-control-plane -am -DskipTests

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the compiled JAR from the builder stage
COPY --from=builder /app/auradb-control-plane/target/auradb-control-plane-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
