package ie.daithi.cards.repositories

import ie.daithi.cards.model.AppUser
import java.util.*
import org.springframework.data.mongodb.repository.MongoRepository

interface AppUserRepo : MongoRepository<AppUser, String> {
    fun findByIdIn(userIds: List<String>): List<AppUser>
}
