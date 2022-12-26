FROM openjdk:19-alpine AS builder

WORKDIR /opt/app

COPY ./gradle ./gradle
COPY ./gradlew ./
COPY ./build.gradle.kts ./
COPY ./settings.gradle ./
COPY ./.env ./
COPY ./system.properties ./

RUN ./gradlew clean build || return 0

COPY ./src ./src

RUN ./gradlew build publishToMavenLocal

FROM openjdk:19-alpine

WORKDIR /opt/app

COPY --from=builder /opt/app/build/libs/cards-110-api.jar /opt/app/app.jar

ENTRYPOINT ["java", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "./app.jar", "-XX:+UseContainerSupport"]
