package ie.daithi.cards.service

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.*
import ie.daithi.cards.model.Game
import ie.daithi.cards.repositories.GameRepo
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File


class GameServiceTest {

    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private val gameRepo: GameRepo = mockk()
    private val deckService: DeckService = mockk()
    private val publishService: PublishService = mockk()

    private val gameService = GameService(gameRepo, deckService, publishService)

    @Nested
    inner class CalculateScoresForRound {

        private val game = objectMapper.readValue(File("src/test/resources/game1.json"), Game::class.java)

        @Test
        fun `round 1 test`()
        {

            val response = gameService.calculateScoresForRound(round = game.completedRounds[0], players = game.players)

            assert(response["player1"] == 10)
            assert(response["player2"] == null)
            assert(response["player3"] == null)
            assert(response["player4"] == 20)
        }

        @Test
        fun `round 2 test`()
        {

            val response = gameService.calculateScoresForRound(round = game.completedRounds[1], players = game.players)

            assert(response["player1"] == null)
            assert(response["player2"] == null)
            assert(response["player3"] == 5)
            assert(response["player4"] == 25)
        }

        @Test
        fun `round 3 test`()
        {

            val response = gameService.calculateScoresForRound(round = game.completedRounds[2], players = game.players)

            assert(response["player1"] == 15)
            assert(response["player2"] == 5)
            assert(response["player3"] == null)
            assert(response["player4"] == 10)
        }

        @Test
        fun `round 4 test`()
        {

            val response = gameService.calculateScoresForRound(round = game.completedRounds[3], players = game.players)

            assert(response["player1"] == 15)
            assert(response["player2"] == null)
            assert(response["player3"] == 5)
            assert(response["player4"] == 10)
        }

        @Test
        fun `round 5 test`()
        {

            val response = gameService.calculateScoresForRound(round = game.completedRounds[4], players = game.players)

            assert(response["player1"] == 20)
            assert(response["player2"] == null)
            assert(response["player3"] == 10)
            assert(response["player4"] == null)
        }

        @Test
        fun `round 6 test`()
        {

            val response = gameService.calculateScoresForRound(round = game.completedRounds[5], players = game.players)

            assert(response["player1"] == null)
            assert(response["player2"] == null)
            assert(response["player3"] == 20)
            assert(response["player4"] == 10)
        }

        @Test
        fun `round 7 test`()
        {

            val response = gameService.calculateScoresForRound(round = game.completedRounds[6], players = game.players)

            assert(response["player1"] == null)
            assert(response["player2"] == 25)
            assert(response["player3"] == 5)
            assert(response["player4"] == null)
        }

        @Test
        fun `round 8 test`()
        {

            val response = gameService.calculateScoresForRound(round = game.completedRounds[7], players = game.players)

            assert(response["player1"] == null)
            assert(response["player2"] == 5)
            assert(response["player3"] == 15)
            assert(response["player4"] == 10)
        }

        @Test
        fun `round 9 test`()
        {

            val response = gameService.calculateScoresForRound(round = game.completedRounds[8], players = game.players)

            assert(response["player1"] == 10)
            assert(response["player2"] == 10)
            assert(response["player3"] == 5)
            assert(response["player4"] == 5)
        }

        @Test
        fun `round 10 test`()
        {

            val response = gameService.calculateScoresForRound(round = game.completedRounds[9], players = game.players)

            assert(response["player1"] == 10)
            assert(response["player2"] == 15)
            assert(response["player3"] == 5)
            assert(response["player4"] == null)
        }

        @Test
        fun `round 11 test`()
        {

            val response = gameService.calculateScoresForRound(round = game.completedRounds[10], players = game.players)

            assert(response["player1"] == null)
            assert(response["player2"] == 30)
            assert(response["player3"] == null)
            assert(response["player4"] == null)
        }

        @Test
        fun `round 12 test`()
        {

            val response = gameService.calculateScoresForRound(round = game.completedRounds[11], players = game.players)

            assert(response["player1"] == 5)
            assert(response["player2"] == 5)
            assert(response["player3"] == null)
            assert(response["player4"] == 20)
        }

        @Test
        fun `round 13 test`()
        {

            val response = gameService.calculateScoresForRound(round = game.completedRounds[12], players = game.players)

            assert(response["player1"] == null)
            assert(response["player2"] == null)
            assert(response["player3"] == 15)
            assert(response["player4"] == 15)
        }

        @Test
        fun `round 14 test`()
        {

            val response = gameService.calculateScoresForRound(round = game.completedRounds[13], players = game.players)

            assert(response["player1"] == null)
            assert(response["player2"] == 5)
            assert(response["player3"] == 20)
            assert(response["player4"] == 5)
        }
    }
}