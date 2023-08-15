FROM openjdk:22-jdk-slim

WORKDIR /app

COPY build/libs/todolist-gateway.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
