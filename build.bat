@ECHO off

ECHO "1. Building cards-110-api"

CALL gradlew.bat build install

ECHO "2. Building image cards-110-api"
CALL docker build --build-arg VERSION=1.2.0-SNAPSHOT -t localhost:5000/cards-110-api:latest -t localhost:5000/cards-110-api:1.2.0-SNAPSHOT .
CALL docker push localhost:5000/cards-110-api:latest
docker push localhost:5000/issuer-api:1.2.0-SNAPSHOT