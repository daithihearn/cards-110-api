FROM openjdk:14-alpine

ARG VERSION
ENV API_VERSION $VERSION

WORKDIR /opt/app

COPY build/libs/cards-110-api-${VERSION}.jar /opt/app/app.jar

ENTRYPOINT ["java", "-jar", "./app.jar", "-XX:+UseContainerSupport", "-Djdk.tls.client.protocols=TLSv1.2"]