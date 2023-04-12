package ie.daithi.cards.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@OpenAPIDefinition(
    info = Info(
        title =
        "Cards 110 API",
        version = "6.0.0",
        description = "The API for the Cards 110 game"
    )
)
@SpringBootApplication(scanBasePackages = ["ie.daithi.cards"])
class CardsApplication

fun main(args: Array<String>) {
    runApplication<CardsApplication>(*args)
}