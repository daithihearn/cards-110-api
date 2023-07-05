package ie.daithi.cards.service

import org.apache.logging.log4j.LogManager
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("dev")
class StubCloudService : CloudService {
    override fun uploadImage(imageUri: String): String {
        logger.warn("Cloud service is not enabled")
        return imageUri
    }

    companion object {
        private val logger = LogManager.getLogger(StubCloudService::class.java)
    }
}
