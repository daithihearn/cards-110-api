package ie.daithi.cards.service

import ie.daithi.cards.repositories.AppUserRepo
import ie.daithi.cards.web.exceptions.NotFoundException
import ie.daithi.cards.model.AppUser
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service

@Service
class AppUserService (
        private val appUserRepo: AppUserRepo,
        private val cloudService: CloudService
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

    fun updateUser(subject: String, name: String, picture: String, forceUpdate: Boolean): AppUser {
        if (logger.isDebugEnabled) logger.debug("Updating user profile for $subject")
        val existingUser = appUserRepo.findById(subject)

        val newUser = if (existingUser.isPresent) {
            val updatedUser = existingUser.get()
            if (forceUpdate || !updatedUser.pictureLocked) updatedUser.picture = picture
            AppUser(id = updatedUser.id, name = updatedUser.name, picture = updatedUser.picture, pictureLocked = updatedUser.pictureLocked || forceUpdate)
        } else
            AppUser(id = subject, name = name, picture = picture, pictureLocked = forceUpdate)

        try {
            if (newUser.picture != null && newUser.picture != "") {
                val cloudImage = cloudService.uploadImage(newUser.picture!!)
                newUser.picture = cloudImage
            }
        } catch (err: Error) {
            logger.error("Failed to upload image to cloud service. Skipping... ${err.message}")
        }

        appUserRepo.save(newUser)
        return newUser
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