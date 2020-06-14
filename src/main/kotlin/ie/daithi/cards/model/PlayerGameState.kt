package ie.daithi.cards.model

import ie.daithi.cards.enumeration.Card
import ie.daithi.cards.enumeration.GameStatus

data class PlayerGameState(
        val me: Player,
        val cards: List<Card>,
        val status: GameStatus,
        val dummy: List<Card>?,
        val round: Round,
        val maxCall: Int?,
        val playerProfiles: List<Player>
)