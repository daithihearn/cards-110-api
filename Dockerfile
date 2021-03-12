FROM openjdk:14-alpine

ARG VERSION
ENV API_VERSION $VERSION

WORKDIR /opt/app

COPY build/libs/cards-110-api-${VERSION}.jar /opt/app/app.jar

ENTRYPOINT ["java", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "./app.jar", "-XX:+UseContainerSupport"]