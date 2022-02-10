package ie.daithi.cards.service

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("dev")
class StubCloudService: CloudService {
    override fun uploadImage(imageUri: String): String {
        return imageUri
    }
}