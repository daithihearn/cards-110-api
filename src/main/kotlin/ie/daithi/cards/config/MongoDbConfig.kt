package ie.daithi.cards.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.MongoTransactionManager
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@DependsOn("mongoTemplate")
@EnableMongoRepositories(basePackages = ["ie.daithi.cards"])
class MongoDbConfig(
        private val mongoTemplate: MongoTemplate
) {

    @Bean
    fun transactionManager(mongoDbFactory: MongoDatabaseFactory): MongoTransactionManager {
        return MongoTransactionManager(mongoDbFactory)
    }
}