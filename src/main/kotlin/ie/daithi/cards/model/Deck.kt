package ie.daithi.cards.model

import ie.daithi.cards.enumeration.Card
import java.util.*
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "deck") data class Deck(@Id val id: String, val cards: Stack<Card>)
