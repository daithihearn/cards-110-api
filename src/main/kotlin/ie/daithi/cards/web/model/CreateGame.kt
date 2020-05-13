package ie.daithi.cards.web.model

data class CreateGame(
        val createPlayers: List<CreatePlayer>,
        val name: String,
        val emailMessage: String
)