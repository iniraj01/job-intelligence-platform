# Build stage (Uses Maven and Java 21 to build the JAR)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src
# Build the application
RUN mvn clean package -DskipTests

# Run stage (Uses a lightweight Java 21 image to run the JAR)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy the built JAR from the previous stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
