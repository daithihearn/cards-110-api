package ie.daithi.cards.model

import ie.daithi.cards.enumeration.RoundStatus
import ie.daithi.cards.enumeration.Suit
import ie.daithi.cards.web.model.Score
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection="round")
data class Round(
        @Id
        val id: String,
        var goer: Player? = null,
        var status: RoundStatus,
        val dealer: Player,
        var currentPlayer: Player,
        var hands: List<Hand>,
        var suit: Suit? = null,
        var cardNumber: Int = 0,
        val scores: List<Score> = emptyList()
)