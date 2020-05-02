package ie.daithi.cards.model

import ie.daithi.cards.enumeration.Card
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document(collection = "appUsers")
data class Deck(
        @Id
        val id: String,
        val cards: Stack<Card>
)