package ie.daithi.cards.repositories

import ie.daithi.cards.enumeration.GameStatus
import ie.daithi.cards.model.Game
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface GameRepo: MongoRepository<Game, String> {
    fun findFirstByPlayersId(playerId: String): Game
    fun findAllByStatusOrStatus(status1: GameStatus, status2: GameStatus): List<Game>
    fun findByPlayersIdAndStatusOrStatus(id: String, active: GameStatus, finished: GameStatus): Game
}