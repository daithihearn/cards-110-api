package ie.daithi.cards.model

import ie.daithi.cards.enumeration.GameStatus
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection="games")
data class Game (
        @Id
        val id: String,
        val timestamp: LocalDateTime,
        val name: String,
        var status: GameStatus,
	    val adminId: String,
        var players: List<Player>,
        var currentRound: Round,
        var completedRounds: List<Round> = emptyList()
)