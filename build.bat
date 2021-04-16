@ECHO off

ECHO "Building image cards-110-api"
CALL docker build -t localhost:5000/cards-110-api:latest .
CALL docker push localhost:5000/cards-110-api:latest