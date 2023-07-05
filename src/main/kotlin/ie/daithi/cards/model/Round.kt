package ie.daithi.cards.model

import ie.daithi.cards.enumeration.RoundStatus
import ie.daithi.cards.enumeration.Suit
import java.time.LocalDateTime

data class Round(
    val timestamp: LocalDateTime,
    val number: Int,
    val dealerId: String,
    var goerId: String? = null,
    var suit: Suit? = null,
    var status: RoundStatus,
    var currentHand: Hand,
    var dealerSeeingCall: Boolean = false,
    var completedHands: List<Hand> = emptyList()
)
