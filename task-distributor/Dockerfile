FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/task-distributor-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8090 9090

ENTRYPOINT ["java", "-jar", "app.jar"]