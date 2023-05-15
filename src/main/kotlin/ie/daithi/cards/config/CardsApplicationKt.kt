package ie.daithi.cards.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.servers.Server
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@OpenAPIDefinition(
    security = [SecurityRequirement(name = "bearerAuth")],
    info = Info(
        title =
        "Cards 110 API",
        version = "6.1.1",
        description = "The API for the Cards 110 game"
    ),
    servers = [
        Server(url = "/", description = "Cards API"),
    ]
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)
@SpringBootApplication(scanBasePackages = ["ie.daithi.cards"])
class CardsApplication

fun main(args: Array<String>) {
    runApplication<CardsApplication>(*args)
}