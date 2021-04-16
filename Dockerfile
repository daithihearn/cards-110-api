FROM openjdk:14-alpine

WORKDIR /opt/app

COPY ./ ./

RUN ./gradlew build install

COPY ./build/libs/cards-110-api-2.3.0-SNAPSHOT.jar /opt/app/app.jar

ENTRYPOINT ["java", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "./app.jar", "-XX:+UseContainerSupport"]