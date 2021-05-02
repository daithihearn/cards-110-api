package ie.daithi.cards.model

import ie.daithi.cards.enumeration.Card
import ie.daithi.cards.enumeration.GameStatus

data class PlayerGameState(
        val id: String,
        val me: Player,
        val isMyGo: Boolean,
        val iamGoer: Boolean,
        val iamDealer: Boolean,
        val cards: List<Card>,
        val status: GameStatus,
        val round: Round,
        val maxCall: Int?,
        val playerProfiles: List<Player>
)