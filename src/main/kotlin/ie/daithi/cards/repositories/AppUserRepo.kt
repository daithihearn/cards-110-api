package ie.daithi.cards.repositories

import ie.daithi.cards.web.security.model.AppUser
import org.springframework.data.mongodb.repository.MongoRepository

interface AppUserRepo: MongoRepository<AppUser, String> {
    fun findByUsernameIgnoreCase(username: String): AppUser?
    fun existsByUsernameIgnoreCase(username: String): Boolean
    fun deleteByUsernameIgnoreCase(username: String)
}