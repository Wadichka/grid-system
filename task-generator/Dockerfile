FROM eclipse-temurin:21-jre

WORKDIR /app

COPY ./target/task-generator-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p "/app/jars"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]