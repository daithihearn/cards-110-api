package ie.daithi.cards.web.model

data class CreateGame(
        val players: List<String>,
        val name: String
)