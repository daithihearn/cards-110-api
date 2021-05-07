package ie.daithi.cards.service

import ie.daithi.cards.enumeration.GameStatus
import ie.daithi.cards.model.Game
import ie.daithi.cards.model.PlayerGameStats
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service

@Service
class StatsService(
    private val mongoOperations: MongoOperations
    ) {

    fun gameStatsForPlayer(playerId: String): List<PlayerGameStats> {
        val match1 = Aggregation.match(Criteria.where("status").`is`(GameStatus.FINISHED).and("players._id").`is`(playerId))
        val unwind = Aggregation.unwind("\$players")
        val match2 = Aggregation.match(Criteria.where("players._id").`is`(playerId))
        val project = Aggregation.project()
            .and("\$id").`as`("gameId")
            .and("\$timestamp").`as`("timestamp")
            .and("\$players.winner").`as`("winner")
            .and("\$players.score").`as`("score")
            .and("\$players.rings").`as`("rings")

        val aggregation = Aggregation.newAggregation(match1, unwind, match2, project)
        return mongoOperations.aggregate(aggregation, Game::class.java, PlayerGameStats::class.java).mappedResults
    }
}