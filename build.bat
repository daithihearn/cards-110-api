@ECHO off

ECHO "1. Building cards-110-api"

gradlew.bat build install

ECHO "2. Building image cards-110-api"
CALL docker build -t localhost:5000/cards-110-api:latest .
CALL docker push localhost:5000/cards-110-api:latest