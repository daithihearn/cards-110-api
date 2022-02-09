package ie.daithi.cards.service

import ie.daithi.cards.repositories.AppUserRepo
import ie.daithi.cards.web.exceptions.NotFoundException
import ie.daithi.cards.model.AppUser
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service

@Service
class AppUserService (
        private val appUserRepo: AppUserRepo
) {

    fun getUser(userId: String): AppUser {
        val appUser = appUserRepo.findById(userId)
        if (!appUser.isPresent) throw NotFoundException("AppUser($userId) not found")
        return appUser.get()
    }

    fun getUsers(players: List<String>): List<AppUser> {
        return appUserRepo.findByIdIn(players)
    }

    fun getAllUsers(): List<AppUser> {
        return appUserRepo.findAll()
    }

    fun exists(id: String): Boolean {
        return appUserRepo.existsById(id)
    }

    fun updateUser(subject: String, name: String, email: String, picture: String?) {
        val existingUser = appUserRepo.findById(subject)

        if (existingUser.isPresent) {
            val newUser = existingUser.get()
            if (!existingUser.get().pictureLocked) newUser.picture = picture

            appUserRepo.save(newUser)
        } else
            appUserRepo.save(AppUser(id = subject, name = name, email = email, picture = picture ))
    }

    fun existsBySubject(subject: String): Boolean {
        return appUserRepo.existsById(subject)
    }

    fun getUserBySubject(subject: String): AppUser {
        return appUserRepo.findById(subject).get()
    }

    companion object {
        private val logger = LogManager.getLogger(AppUserService::class.java)
    }
}