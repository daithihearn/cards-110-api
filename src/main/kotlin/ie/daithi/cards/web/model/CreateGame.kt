package ie.daithi.cards.web.model

data class CreateGame(
        val playerEmails: List<String>,
        val name: String
)