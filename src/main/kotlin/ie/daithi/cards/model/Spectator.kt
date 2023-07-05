package ie.daithi.cards.model

import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "spectator")
data class Spectator(@Id val id: String, val gameId: String, val timestamp: LocalDateTime)
