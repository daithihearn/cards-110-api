package ie.daithi.cards.model

import java.time.LocalDateTime
import java.time.ZoneOffset
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "appUsers")
data class AppUser(
    @Id var id: String? = null,
    val name: String,
    var picture: String? = null,
    val pictureLocked: Boolean = false,
    val lastAccess: LocalDateTime = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC),
)
