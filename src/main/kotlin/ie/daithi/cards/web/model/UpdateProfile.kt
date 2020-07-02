package ie.daithi.cards.web.model

data class UpdateProfile (
    val name: String,
    val email: String,
    val picture: String? = null
)