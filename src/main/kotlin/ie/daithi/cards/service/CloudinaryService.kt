package ie.daithi.cards.service

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import org.apache.commons.codec.digest.DigestUtils
import org.apache.logging.log4j.LogManager
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("prod")
class CloudinaryService(
        private val cloudinary: Cloudinary) : CloudService {

    override fun uploadImage(imageUri: String): String {
        if (imageUri.contains("res.cloudinary.com")) {
            logger.info("Skipping image upload as it is cloudinary URL $imageUri")
            return imageUri
        }

        val publicId = "cards/avatars/${DigestUtils.md5Hex(imageUri)}"

        logger.info("Uploading new Avatar: $publicId ")

        // Can we check if it already exists here?

        val params = ObjectUtils.asMap(
                "public_id", publicId,
                "overwrite", true,
                "resource_type", "image"
        )

        return cloudinary.uploader().upload(imageUri, params)["secure_url"] as String
    }

    companion object {
        private val logger = LogManager.getLogger(CloudinaryService::class.java)
    }
}