package ie.daithi.cards.model

import java.time.LocalDateTime

data class PlayerGameStats(
                val gameId: String,
                val timestamp: LocalDateTime,
                var winner: Boolean,
                var score: Int,
                var rings: Int?
)
