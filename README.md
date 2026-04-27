# Spring Boot REST API Demo

A simple Spring Boot REST API project with Spring Web and Lombok dependencies.

## Project Structure

```
.
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/
│   │   │   ├── DemoApplication.java
│   │   │   └── TestController.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/java/com/example/demo/
```

## Dependencies

- **Spring Boot Starter Web**: For building REST APIs
- **Lombok**: For reducing boilerplate code
- **Spring Boot Starter Test**: For testing

## API Endpoints

### GET /test
Returns a simple message confirming the backend is running.

**Response:**
```
"Backend is running"
```

## Building the Project

```bash
mvn clean install
```

## Running the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Testing the API

```bash
curl http://localhost:8080/test
```

Expected response:
```
Backend is running
```
