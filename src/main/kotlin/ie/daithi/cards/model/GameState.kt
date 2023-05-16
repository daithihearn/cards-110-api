package ie.daithi.cards.model

import ie.daithi.cards.enumeration.Card
import ie.daithi.cards.enumeration.GameStatus

data class GameState(
        val id: String,
        val me: Player? = null,
        val iamSpectator: Boolean,
        val isMyGo: Boolean,
        val iamGoer: Boolean,
        val iamDealer: Boolean,
        val iamAdmin: Boolean,
        val cards: List<Card>? = null,
        val status: GameStatus,
        val round: Round,
        val maxCall: Int? = null,
        val players: List<Player>
)