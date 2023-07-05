package ie.daithi.cards.model

import ie.daithi.cards.web.model.enums.EventType

data class PublishContent(
    val gameState: GameState,
    val transitionData: Any? = null,
    val type: EventType
)
