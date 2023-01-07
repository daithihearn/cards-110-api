# Cards 110 API
The API layer for the [Cards 110 application](https://github.com/daithihearn/cards-110)

# Requirements
To run this application you need a MongoDB and Redis instance to point to.
You will also need an application and API configured in Auth0.
If you are running with the prod spring profile you will also need a cloudinary account.

All of the above can be configured in the `.env` file.

# Technical Stack
- Kotlin
- Spring-Boot
- Swagger
- MongoDB

# Building
To build locally run `./gradlew clean build`
To build the docker image run `docker build . -t cards110-api`

# Running
To run locally built docker image run `docker run -d -p 8080:8080 --name cards110-api cards110-api`