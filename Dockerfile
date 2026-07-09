FROM openjdk:21-jdk-slim

WORKDIR /app

COPY target/AIAgent-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["java", "-jar", "app.jar"]
