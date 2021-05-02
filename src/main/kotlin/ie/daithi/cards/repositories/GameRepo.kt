package ie.daithi.cards.repositories

import ie.daithi.cards.enumeration.GameStatus
import ie.daithi.cards.model.Game
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import java.util.*

interface GameRepo: MongoRepository<Game, String> {
    @Query(value = "{ 'players.id' : ?0, 'status': ?1 }")
    fun findByPlayerIdAndStatus(playerId: String, status: GameStatus): List<Game>
    fun findAllByAdminIdAndStatusOrStatus(adminId: String, status1: GameStatus, status2: GameStatus): List<Game>
    fun findByPlayersIdAndStatusOrStatus(id: String, active: GameStatus, finished: GameStatus): List<Game>
    fun findByAdminIdAndStatusOrStatus(id: String, active: GameStatus, finished: GameStatus): List<Game>

    @Query(value = "{'\$and': [{'\$or': [{'players._id': ?0},{'adminId': ?0}]},{'\$or': [ {'status': 'FINISHED'}, {'status': 'ACTIVE'}, {'status': 'COMPLETED'}]}]}")
    fun getMyActive(userId: String): List<Game>
}