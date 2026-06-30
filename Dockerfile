# Build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline -DskipTests
COPY src src
RUN mvn -q package -DskipTests

# Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R app:app /app
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
