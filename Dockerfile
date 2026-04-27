# Build stage (Uses Maven and Java 21 to build the JAR)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src
# Build the application
RUN mvn clean package -DskipTests

# Run stage (Uses a lightweight Java 21 image to run the JAR)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install Python3 and dependencies for job fetching and resume parsing scripts
RUN apt-get update && apt-get install -y python3 python3-pip && rm -rf /var/lib/apt/lists/*
RUN pip3 install requests PyPDF2 spacy
RUN python3 -m spacy download en_core_web_sm

# Copy the built JAR from the previous stage
COPY --from=build /app/target/*.jar app.jar

# Copy python scripts
COPY job_fetcher.py .
COPY extract_skills.py .

EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
