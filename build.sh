#!/bin/sh

CARDS_API_VERSION=$(sed -n 's/^ISSUER_VERSION=//p' .env)

echo "
1. Building jar cards-110-api"

./gradlew build install

if [ -x "$(command -v docker)" ]; then
	echo "
	2. Building image cards-110-api"
	docker build --build-arg VERSION=${CARDS_API_VERSION} -t localhost:5000/cards-110-api:latest -t localhost:5000/cards-110-api:${CARDS_API_VERSION} .
	docker push localhost:5000/cards-110-api:latest
	docker push localhost:5000/issuer-api:${CARDS_API_VERSION}
else
    echo "Docker not installed"
fi