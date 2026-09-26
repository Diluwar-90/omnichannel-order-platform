FROM eclipse-temurin:21-jre

WORKDIR /app

COPY build/libs/order-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]