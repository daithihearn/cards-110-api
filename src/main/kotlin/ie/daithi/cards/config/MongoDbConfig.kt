package ie.daithi.cards.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import javax.annotation.PostConstruct

@Configuration
@DependsOn("mongoTemplate")
@EnableMongoRepositories(basePackages = ["ie.daithi.cards"])
class MongoDbConfig(
        private val mongoTemplate: MongoTemplate
) {

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