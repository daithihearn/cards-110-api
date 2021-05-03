package ie.daithi.cards.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "spectator")
data class Spectator(
        @Id
        val id: String,
        val gameId: String,
        val timestamp: LocalDateTime
)