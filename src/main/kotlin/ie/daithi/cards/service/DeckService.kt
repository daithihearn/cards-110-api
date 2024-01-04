package ie.daithi.cards.service

import ie.daithi.cards.enumeration.Card
import ie.daithi.cards.model.*
import ie.daithi.cards.repositories.DeckRepo
import ie.daithi.cards.web.exceptions.NotFoundException
import java.security.SecureRandom
import java.util.*
import org.springframework.stereotype.Service

@Service
class DeckService(private val deckRepo: DeckRepo) {

    // Shuffle the deck for a game
    fun shuffle(gameId: String) {
        val deckList = Card.values().toList().shuffled(SecureRandom())
        val deck = Stack<Card>()
        deck.addAll(deckList.filter { card -> card != Card.EMPTY })
        deckRepo.save(Deck(id = gameId, cards = deck))
    }

    fun save(deck: Deck) {
        deckRepo.save(deck)
    }

    fun getDeck(gameId: String): Deck {
        val deckOpt = deckRepo.findById(gameId)
        if (!deckOpt.isPresent) throw NotFoundException("Deck not found")
        return deckOpt.get()
    }
}
