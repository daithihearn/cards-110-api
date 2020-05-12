package ie.daithi.cards.model

import ie.daithi.cards.enumeration.Card

data class Player(
        val id: String,
        val displayName: String,
        var call: Int = 0,
        var cards: List<Card> = emptyList(),
        var score: Int = 0,
        val teamId: String
)