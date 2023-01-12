package ie.daithi.cards.model

import ie.daithi.cards.enumeration.Card

data class PlayedCard(
        val playerId: String,
        var card: Card,

)