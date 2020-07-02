package ie.daithi.cards.model

import ie.daithi.cards.web.model.enums.EventType

data class PublishContent(
        val content: Any,
        val type: EventType
)