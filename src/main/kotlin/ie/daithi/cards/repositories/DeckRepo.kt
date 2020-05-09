package ie.daithi.cards.repositories

import ie.daithi.cards.model.Deck
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface DeckRepo: CrudRepository<Deck, String>