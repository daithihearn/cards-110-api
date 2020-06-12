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
        if (appUser.isEmpty) throw NotFoundException("AppUser($userId) not found")
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
        // 1. Check if a user exists with this email address
        val existingUser = appUserRepo.findOneByEmail(email)

        val appUser = if (existingUser.isPresent)
            AppUser(id = existingUser.get().id, subject = subject, name = name, email = email, picture = picture)
        else
            AppUser(subject = subject, name = name, email = email, picture = picture)

        // 2. Save record
        appUserRepo.save(appUser)
    }

    fun existsBySubject(subject: String): Boolean {
        return appUserRepo.existsBySubject(subject)
    }

    fun getUserBySubject(subject: String): AppUser {
        return appUserRepo.findOneBySubject(subject)
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java)
    }
}