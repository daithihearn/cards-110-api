package ie.daithi.cards.repositories

import ie.daithi.cards.model.Spectator
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SpectatorRepo: CrudRepository<Spectator, String> {
    fun findAllByGameId(gameId: String): List<Spectator>
}