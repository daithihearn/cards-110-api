package ie.daithi.cards.repositories

import ie.daithi.cards.enumeration.GameStatus
import ie.daithi.cards.model.Game
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface GameRepo: MongoRepository<Game, String> {
    @Query(value = "{ 'players.id' : ?0 }")
    fun getByPlayerId(playerId: String): Game
    fun findAllByStatus(started: GameStatus): List<Game>

}