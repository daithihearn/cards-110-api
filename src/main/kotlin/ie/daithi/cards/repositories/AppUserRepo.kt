package ie.daithi.cards.repositories

import ie.daithi.cards.model.AppUser
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.*

interface AppUserRepo: MongoRepository<AppUser, String> {
    fun findByIdIn(userIds: List<String>): List<AppUser>
    fun findOneByEmail(email: String): Optional<AppUser>
}