package ie.daithi.cards.model

import ie.daithi.cards.enumeration.Card
import ie.daithi.cards.enumeration.RoundStatus
import ie.daithi.cards.enumeration.Suit
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection="round")
data class Round(
        @Id
        val id: String,
        var status: RoundStatus,
        var goer: Player? = null,
        var suit: Suit? = null,
        var leadOut: Card? = null,
        val dealer: Player,
        var currentPlayer: Player,
        var currentHand: Map<String, Card> = emptyMap(),
        var completedHands: List<Map<String, Card>> = emptyList()
)