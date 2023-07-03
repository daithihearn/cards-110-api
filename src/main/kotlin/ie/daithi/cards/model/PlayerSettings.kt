package ie.daithi.cards.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "playerSettings")
data class PlayerSettings(
    @Id
    val playerId: String,
    var autoBuyCards: Boolean = false,
)
