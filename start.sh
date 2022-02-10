#!/bin/bash

echo "
    Setting up the environment variables"
export PORT=8080
export MONGODB_URI=mongodb://mongodb:27017/cards-110
export SPRING_PROFILES_ACTIVE=primary,dev
export PLAYER_LOGIN_URL=http://localhost:8080/\#/autologin
export CORS_WHITELIST=http://localhost:3000
export AUTH0_AUDIENCE=http://localhost:8080
export AUTH0_CLIENT_ID=Issuer URI Goes here

echo "
    Starting app...."
./gradlew bootrun