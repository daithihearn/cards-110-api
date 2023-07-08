package ie.daithi.cards.model

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "playerSettings")
data class PlayerSettings(
    @Id @JsonIgnore var playerId: String? = null,
    var autoBuyCards: Boolean = true,
)
