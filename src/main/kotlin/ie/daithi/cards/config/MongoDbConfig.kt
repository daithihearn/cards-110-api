package ie.daithi.cards.config

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import javax.annotation.PostConstruct

@Configuration
@EnableMongoRepositories(basePackages = ["ie.daithi.cards"])
class MongoDbConfig (
        @Value("\${mongodb.uri}")
        private val mongoDbUri: String,
        @Value("\${mongodb.dbname}")
        private val dbname: String
): AbstractMongoClientConfiguration() {

    override fun getDatabaseName(): String {
        return dbname
    }

    override fun mongoClient(): MongoClient {
        return MongoClients.create(mongoDbUri)
    }

    @PostConstruct
    fun initIndices() {
//        val mongoOps = mongoTemplate()
//
//        mongoOps.indexOps(Answer::class.java).ensureIndex(Index().on("playerId", Sort.Direction.ASC)
//                .on("gameId", Sort.Direction.ASC)
//                .on("roundId", Sort.Direction.ASC)
//                .on("questionId", Sort.Direction.ASC)
//                .unique().sparse())
    }
}