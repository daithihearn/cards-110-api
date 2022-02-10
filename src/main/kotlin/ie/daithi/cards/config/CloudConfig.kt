package ie.daithi.cards.config

import com.cloudinary.Cloudinary
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("prod")
class CloudConfig(
        @Value("\${cloudinary.url}")
        private val cloudinaryUrl: String
) {

    @Bean("cloudinary")
    fun cloudinary(): Cloudinary {
        return Cloudinary(cloudinaryUrl)
    }
}