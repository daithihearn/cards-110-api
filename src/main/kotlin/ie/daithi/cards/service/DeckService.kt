package ie.daithi.cards.service

import ie.daithi.cards.enumeration.Card
import ie.daithi.cards.model.Deck
import ie.daithi.cards.repositories.redis.DeckRepo
import ie.daithi.cards.web.exceptions.NotFoundException
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.*

@Service
class DeckService(
        private val deckRepo: DeckRepo
) {

    // Shuffle the deck for a game
    fun shuffle(gameId: String) {

        val deckList = Card.values().toList().shuffled(SecureRandom())
        val deck = Stack<Card>()
        deck.addAll(deckList)
        deckRepo.save(
                Deck(id = gameId,
                cards = deck)
        )
    }

    // Get the next card
    fun nextCard(gameId: String): Card {
        val deckOpt = deckRepo.findById(gameId)
        if (!deckOpt.isPresent) throw NotFoundException("Deck not found")
        val deck = deckOpt.get()
        if (deck.cards.empty()) throw NotFoundException("No cards left")
        val card = deck.cards.pop()
        deckRepo.save(deck)
        return card
    }
}