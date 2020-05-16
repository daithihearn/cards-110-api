package ie.daithi.cards.service

import ie.daithi.cards.repositories.AppUserRepo
import ie.daithi.cards.web.exceptions.NotFoundException
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
        val appUser = appUserRepo.findById(username)
        if (appUser.isEmpty)
            throw NotFoundException("User not found")

        if (appUser.get().authorities == null)
            throw RuntimeException("appuser not found")

        val authorities = arrayListOf<GrantedAuthority>()
        appUser.get().authorities!!.forEach { authority -> authorities.add(SimpleGrantedAuthority(authority.toString())) }

        return User(appUser.get().username, appUser.get().password, authorities)
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java)
    }
}