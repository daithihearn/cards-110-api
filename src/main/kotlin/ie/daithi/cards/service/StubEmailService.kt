package ie.daithi.cards.service

import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("dev")
class StubEmailService(
        @Value("\${player.login.url}")
        private val playerLoginUrl: String
): EmailService {

    override fun sendQuizInvite(recipientEmail: String, password: String) {
        logger.warn("As you are in dev mode, email will not send.")
        logger.warn("Logging user credentials instead: $playerLoginUrl?username=$recipientEmail&password=$password")
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java)
    }
}