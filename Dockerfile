# Stage 1: Build stage with Maven and Java 17
FROM maven:3.9-eclipse-temurin-17 AS build

# Set the working directory for the build
WORKDIR /app

# Copy pom.xml and repository source code
COPY pom.xml .
COPY src ./src

# Compile and package the application into an executable shaded JAR
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage with Java 17 JRE
FROM eclipse-temurin:17-jre

# Set the working directory inside the container
WORKDIR /app

# Copy the generated executable shaded JAR from the build stage
COPY --from=build /app/target/expense-tracker-1.0-SNAPSHOT.jar app.jar

# Support Render's dynamic PORT environment variable (defaulting to 8080)
ENV PORT=8080
EXPOSE 8080

# Database credentials (DB_HOST, DB_PORT, DB_USER, DB_PASSWORD) are supplied at runtime via environment variables
# No sensitive credentials or database passwords are hardcoded or copied into the image

# Run the packaged Expense Tracker application
ENTRYPOINT ["java", "-jar", "app.jar"]
