# Use official Eclipse Temurin JRE 17 as base image
FROM eclipse-temurin:17-jre

# Set the working directory inside the container
WORKDIR /app

# Copy the packaged executable JAR into the container
COPY target/expense-tracker-1.0-SNAPSHOT.jar app.jar

# DB_PASSWORD should be supplied at runtime via environment variable
# No sensitive credentials or passwords are hardcoded here
ENV DB_PASSWORD=""

# Run the packaged Expense Tracker application
ENTRYPOINT ["java", "-jar", "app.jar"]
