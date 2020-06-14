package ie.daithi.cards.model

import com.fasterxml.jackson.annotation.JsonIgnore
import ie.daithi.cards.enumeration.Card

data class Player(
        val id: String,
        val seatNumber: Int,
        var call: Int = 0,
        @JsonIgnore
        var cards: List<Card> = emptyList(),
        var cardsBought: Int? = null,
        var score: Int = 0,
        val teamId: String
)