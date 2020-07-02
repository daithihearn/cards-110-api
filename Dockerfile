FROM openjdk:13-alpine

WORKDIR /opt/app

COPY build/libs/cards-110-api-1.0.0-SNAPSHOT.jar /opt/app/app.jar

ENTRYPOINT ["java", "-jar", "./app.jar", "-XX:+UseContainerSupport"]