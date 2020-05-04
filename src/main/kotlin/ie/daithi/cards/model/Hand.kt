package ie.daithi.cards.model

import ie.daithi.cards.enumeration.Card

data class Hand(
        val player: Player,
        var call: Int = 0,
        var cards: List<Card> = emptyList(),
        var played: List<Card> = emptyList()
)