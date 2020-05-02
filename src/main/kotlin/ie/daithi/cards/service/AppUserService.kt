package ie.daithi.cards.service

import ie.daithi.cards.repositories.mongodb.AppUserRepo
import org.apache.logging.log4j.LogManager
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import java.lang.RuntimeException

@Service
class AppUserService (
        private val appUserRepo: AppUserRepo
): UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val appUser = appUserRepo.findByUsernameIgnoreCase(username)

        if (appUser?.authorities == null)
            throw RuntimeException("appuser not found")

        val authorities = arrayListOf<GrantedAuthority>()
        appUser.authorities!!.forEach { authority -> authorities.add(SimpleGrantedAuthority(authority.toString())) }

        return User(appUser.username, appUser.password, authorities)
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java)
    }
}