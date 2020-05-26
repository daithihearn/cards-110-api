#!/bin/sh

echo "
1. Building jar cards-110-api"

./gradlew build install

echo "
2. Building image cards-110-api"

docker build -t localhost:5000/cards-110-api:latest .