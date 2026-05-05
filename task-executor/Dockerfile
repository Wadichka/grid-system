FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/task-executor-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p "/app/bundles"

EXPOSE 8091 9091

ENTRYPOINT ["java", "-jar", "app.jar"]