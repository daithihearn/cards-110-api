FROM openjdk:14-alpine AS builder

WORKDIR /opt/app

COPY ./ ./

RUN ./gradlew build install

FROM openjdk:14-alpine

WORKDIR /opt/app

COPY --from=builder /opt/app/build/libs/cards-110-api-3.0.0-SNAPSHOT.jar /opt/app/app.jar

ENTRYPOINT ["java", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "./app.jar", "-XX:+UseContainerSupport"]