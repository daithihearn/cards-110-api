package ie.daithi.cards.config

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class CardsApplication

fun main(args: Array<String>) {
    SpringApplication.run(CardsApplication::class.java, *args)
}