package ie.daithi.cards.model

import ie.daithi.cards.enumeration.GameStatus
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection="games")
data class Game (
    @Id
    val id: String? = null,
    val name: String,
    var status: GameStatus,
    val players: List<Player>,
    val completedRounds: List<Round> = emptyList()
)