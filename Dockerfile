FROM openjdk:12-alpine

WORKDIR /opt/app

COPY build/libs/cards-110-api-0.0.1-SNAPSHOT.jar /opt/app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "./app.jar", "-XX:+UseContainerSupport"]