package ie.daithi.cards.config

import ie.daithi.cards.repositories.mongodb.AppUserRepo
import ie.daithi.cards.web.security.model.AppUser
import ie.daithi.cards.web.security.model.Authority
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
@Profile("primary")
class MongoDbInitData (
        private val appUserRepo: AppUserRepo,
        private val passwordEncoder: BCryptPasswordEncoder,
        @Value("\${admin.username}")
        private val username: String,
        @Value("\${admin.password}")
        private val password: String
){
    @PostConstruct
    fun initAdminUser() {

        // Only initialise the admin user if they aren't in the database
        if (appUserRepo.existsByUsernameIgnoreCase(username)) {
            logger.info("User $username is already in the database so won't create new user")
            return
        }

        logger.info("User $username not found so will create a new user")
        val admin = AppUser(username = username,
                password = passwordEncoder.encode(password),
                authorities = listOf(Authority.ADMIN))

        appUserRepo.save(admin)
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java)
    }
}