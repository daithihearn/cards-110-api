package ie.daithi.cards.model

import ie.daithi.cards.enumeration.Card
import java.time.LocalDateTime

data class Hand(
        val timestamp: LocalDateTime,
        var leadOut: Card? = null,
        var currentPlayerId: String,
        var playedCards: Map<String, Card> = emptyMap()
)