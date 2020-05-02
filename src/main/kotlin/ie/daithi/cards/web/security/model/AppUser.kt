package ie.daithi.cards.web.security.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "appUsers")
data class AppUser (
    @Id
    var id: String? = null,
    @Indexed(unique = true)
    var username: String? = null,
    var password: String? = null,
    var authorities: List<Authority>?
)