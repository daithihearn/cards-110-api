package ie.daithi.cards.model

import ie.daithi.cards.web.model.enums.EventType
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection="currentContent")
data class PublishContent(
        @Id
        var gameId: String? = null,
        var content: Any? = null,
        var type: EventType? = null
)