package ie.daithi.cards.config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["ie.daithi.cards"])
class CardsApplication

fun main(args: Array<String>) {
    runApplication<CardsApplication>(*args)
}