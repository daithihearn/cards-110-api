package ie.daithi.cards.service

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("prod")
class CloudinaryService(
        private val cloudinary: Cloudinary) : CloudService {

    override fun uploadImage(imageUri: String): String {
        val publicId = "cards/avatars/${DigestUtils.md5Hex(imageUri)}"

        // Can we check if it already exists here?

        val params = ObjectUtils.asMap(
                "public_id", publicId,
                "overwrite", true,
                "resource_type", "image"
        )

        return cloudinary.uploader().upload(imageUri, params)["secure_url"] as String
    }

}