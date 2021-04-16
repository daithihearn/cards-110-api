#!/bin/sh

if [ -x "$(command -v docker)" ]; then
	echo "
	Building image cards-110-api"
	docker build -t localhost:5000/cards-110-api:latest .
	docker push localhost:5000/cards-110-api:latest
else
    echo "Docker not installed"
fi