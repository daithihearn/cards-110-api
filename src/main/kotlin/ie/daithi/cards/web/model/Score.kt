package ie.daithi.cards.web.model

import ie.daithi.cards.model.Player

data class Score(
        val player: Player,
        val score: Int
)