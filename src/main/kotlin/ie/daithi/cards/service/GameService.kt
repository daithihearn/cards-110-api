package ie.daithi.cards.service

import ie.daithi.cards.enumeration.Card
import ie.daithi.cards.enumeration.GameStatus
import ie.daithi.cards.enumeration.RoundStatus
import ie.daithi.cards.enumeration.Suit
import ie.daithi.cards.model.*
import ie.daithi.cards.repositories.GameRepo
import ie.daithi.cards.web.exceptions.InvalidOperationException
import ie.daithi.cards.web.exceptions.InvalidStatusException
import ie.daithi.cards.web.exceptions.NotFoundException
import ie.daithi.cards.web.model.enums.EventType
import java.time.LocalDateTime
import java.util.*
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GameService(
        private val gameRepo: GameRepo,
        private val gameUtils: GameUtils,
        private val deckService: DeckService
) {
    @Transactional
    fun create(adminId: String, name: String, playerIds: List<String>): Game {
        logger.info("Attempting to start a game of 110")

        // 1. Validate number of players
        if (playerIds.size !in 2..6) throw InvalidOperationException("Please add 2-6 players")

        // 2. Shuffle players
        val playerIdsShuffled = playerIds.shuffled()

        // 3. Create Players
        val players = arrayListOf<Player>()
        // If we have six players we will assume this is a team game
        if (playerIdsShuffled.size == 6) {
            val teamIds =
                    listOf(
                            UUID.randomUUID().toString(),
                            UUID.randomUUID().toString(),
                            UUID.randomUUID().toString()
                    )

            players.add(Player(id = playerIdsShuffled[0], seatNumber = 1, teamId = teamIds[0]))
            players.add(Player(id = playerIdsShuffled[1], seatNumber = 2, teamId = teamIds[1]))
            players.add(Player(id = playerIdsShuffled[2], seatNumber = 3, teamId = teamIds[2]))
            players.add(Player(id = playerIdsShuffled[3], seatNumber = 4, teamId = teamIds[0]))
            players.add(Player(id = playerIdsShuffled[4], seatNumber = 5, teamId = teamIds[1]))
            players.add(Player(id = playerIdsShuffled[5], seatNumber = 6, teamId = teamIds[2]))
        } else {
            playerIdsShuffled.forEachIndexed { index, playerId ->
                players.add(Player(id = playerId, seatNumber = index + 1, teamId = playerId))
            }
        }

        // 5. Create the first round and assign a dealer
        val timestamp = LocalDateTime.now()
        val dealerId = players[0].id
        val hand =
                Hand(
                        timestamp = timestamp,
                        currentPlayerId = gameUtils.nextPlayer(players, dealerId).id
                )

        val round =
                Round(
                        timestamp = timestamp,
                        number = 1,
                        status = RoundStatus.CALLING,
                        currentHand = hand,
                        dealerId = dealerId
                )

        // 6. Create Game
        var game =
                Game(
                        id = UUID.randomUUID().toString(),
                        timestamp = timestamp,
                        name = name,
                        status = GameStatus.ACTIVE,
                        adminId = adminId,
                        players = players,
                        currentRound = round
                )

        // 7. Deal Cards
        gameUtils.dealCards(game)

        // 8. Save the game
        game = save(game)

        logger.info("Game started successfully ${game.id}")
        return game
    }

    fun get(id: String): Game {
        val game = gameRepo.findById(id)
        if (!game.isPresent) throw NotFoundException("Game $id not found")
        return game.get()
    }

    fun save(game: Game): Game {
        return gameRepo.save(game)
    }

    fun delete(id: String) {
        gameRepo.deleteById(id)
    }

    fun getAll(): List<Game> {
        return gameRepo.findAll()
    }

    fun getMyGames(userId: String): List<Game> {
        return gameRepo.getMyGames(userId)
    }

    fun getActiveForAdmin(adminId: String): List<Game> {
        return gameRepo.findByAdminIdAndStatusOrStatus(
                adminId,
                GameStatus.ACTIVE,
                GameStatus.FINISHED
        )
    }

    @Transactional
    fun cancel(id: String) {
        val game = get(id)
        if (game.status == GameStatus.CANCELLED)
                throw InvalidStatusException("Game is already in CANCELLED state")
        game.status = GameStatus.CANCELLED
        save(game)
    }

    @Transactional
    fun replay(currentGame: Game, playerId: String) {

        val timestamp = LocalDateTime.now()

        // 1. Check if they are the dealer
        val dealer = currentGame.currentRound.dealerId
        if (dealer != playerId)
                throw InvalidOperationException("Player $playerId is not the dealer")

        // 2. Set current game as completed
        currentGame.status = GameStatus.COMPLETED
        save(currentGame)

        // 3. Get players and
        val oldPlayers = currentGame.players.shuffled()
        val players = arrayListOf<Player>()
        // If we have six players we will assume this is a team game
        if (oldPlayers.size == 6) {
            val teamIds =
                    listOf(
                            UUID.randomUUID().toString(),
                            UUID.randomUUID().toString(),
                            UUID.randomUUID().toString()
                    )
            oldPlayers.forEachIndexed { index, player ->
                players.add(
                        Player(id = player.id, seatNumber = index + 1, teamId = teamIds[3 % index])
                )
            }
        } else {
            oldPlayers.forEachIndexed { index, player ->
                // For an individual game set the team ID == player ID
                players.add(Player(id = player.id, seatNumber = index + 1, teamId = player.id))
            }
        }

        // 4. Create the first round and assign a dealer
        val dealerId = players[0].id
        val hand =
                Hand(
                        timestamp = timestamp,
                        currentPlayerId = gameUtils.nextPlayer(players, dealerId).id
                )

        val round =
                Round(
                        timestamp = timestamp,
                        number = 1,
                        status = RoundStatus.CALLING,
                        currentHand = hand,
                        dealerId = dealerId
                )

        // 5. Create Game
        var game =
                Game(
                        id = UUID.randomUUID().toString(),
                        timestamp = timestamp,
                        name = currentGame.name,
                        status = GameStatus.ACTIVE,
                        adminId = currentGame.adminId,
                        players = players,
                        currentRound = round
                )

        // 6. Save the game
        game = save(game)

        // 7. Publish updated game
        gameUtils.publishGame(game = game, type = EventType.REPLAY)
    }

    @Transactional
    fun call(gameId: String, playerId: String, call: Int) {
        // 1. Validate call value
        if (!VALID_CALL.contains(call))
                throw InvalidOperationException("Call value $call is invalid")

        // 2. Get Game
        val game = get(gameId)

        // 3. Get current round
        val currentRound = game.currentRound
        if (currentRound.status != RoundStatus.CALLING)
                throw InvalidOperationException("Can only call in the calling phase")

        // 4. Get current hand
        val currentHand = currentRound.currentHand

        // 5. Get myself
        val me = gameUtils.findPlayer(game.players, currentHand.currentPlayerId)

        // 6. Check they are the next player
        if (currentHand.currentPlayerId != playerId)
                throw InvalidOperationException("It's not your go!")

        // 7. Are they in the bunker? If they are on -30 or less then they can only call 0
        if (me.score < BUNKER_SCORE && call != 0)
                throw InvalidOperationException("You are in the bunker!")

        // 8. Check if call value is valid i.e. > all other calls
        if (currentRound.dealerSeeingCall) {
            me.call = call
        } else if (call != 0) {
            val currentCaller =
                    game.players.maxByOrNull { it.call } ?: throw Exception("Can't find caller")
            if (currentHand.currentPlayerId == currentRound.dealerId && call >= currentCaller.call)
                    me.call = call
            else if (call > currentCaller.call) me.call = call
            else throw InvalidOperationException("Call must be higher than ${currentCaller.call}")
        }

        // 9. Update my call
        game.players.forEach { if (it.id == me.id) it.call = me.call }

        // 10. Set next player/round status
        var type = if (call > 0) EventType.CALL else EventType.PASS

        if (call == 30) {
            logger.info("Jink called by $me")
            if (currentHand.currentPlayerId == currentRound.dealerId) {
                // If the dealer calls jink then that's that
                currentRound.status = RoundStatus.CALLED
                currentRound.goerId = me.id
                currentHand.currentPlayerId = me.id
            } else {
                // If anyone other than the dealer calls JINK. Go to the dealer so that can take it.
                currentHand.currentPlayerId = currentRound.dealerId
            }
        } else if (currentHand.currentPlayerId == currentRound.dealerId) {

            logger.info("Everyone has called")

            val caller =
                    game.players.maxByOrNull { it.call }
                            ?: throw Exception("This should never happen")

            if (caller.call == 0 && me.call == 0) {
                logger.info("Nobody called anything. Will have to re-deal.")
                gameUtils.completeRound(game)
                type = EventType.ROUND_COMPLETED
            } else if (me.call == caller.call && me.id != caller.id) {
                logger.info("Dealer saw a call. Go back to caller.")
                currentHand.currentPlayerId = caller.id
                currentRound.dealerSeeingCall = true
            } else if (caller.call == 10) {
                logger.info("Can't go 10")
                gameUtils.completeRound(game)
                type = EventType.ROUND_COMPLETED
            } else {
                logger.info("Successful call $caller")
                currentRound.status = RoundStatus.CALLED
                currentRound.goerId = caller.id
                currentHand.currentPlayerId = caller.id
            }
        } else if (currentRound.dealerSeeingCall) {
            logger.info("This player was taken by the dealer.")
            if (me.call == 0) {
                logger.info("${me.id} let the dealer go")
                currentRound.status = RoundStatus.CALLED
                currentRound.goerId = currentRound.dealerId
                currentHand.currentPlayerId = currentRound.dealerId
            } else {
                val caller =
                        game.players.maxByOrNull { it.call }
                                ?: throw Exception("This should never happen")
                if (caller.id != me.id) throw InvalidOperationException("Invalid call")
                logger.info("${me.id} has raised the call")
                currentHand.currentPlayerId = currentRound.dealerId
                currentRound.dealerSeeingCall = false
            }
        } else {
            logger.info("Still more players to call")
            currentHand.currentPlayerId = gameUtils.nextPlayer(game.players, me.id).id
        }

        // 11. Save game
        save(game)

        // 12. Publish updated game
        gameUtils.publishGame(game = game, type = type)
    }

    @Transactional
    fun chooseFromDummy(gameId: String, playerId: String, selectedCards: List<Card>, suit: Suit) {
        // 1. Get Game
        val game = get(gameId)

        // 2. Validate number of cards (-1 because of the dummy)
        gameUtils.validateNumberOfCardsSelectedWhenBuying(selectedCards.size, game.players.size - 1)

        // 3. Get Round
        val currentRound = game.currentRound
        if (currentRound.status != RoundStatus.CALLED)
                throw InvalidOperationException("Can only call in the called phase")

        // 4. Get current hand
        val currentHand = currentRound.currentHand

        // 5. Validate player
        if (currentRound.goerId != playerId || currentHand.currentPlayerId != playerId)
                throw InvalidOperationException(
                        "Player ($playerId) is not the goer and current player"
                )

        // 6. Get myself
        val me = gameUtils.findPlayer(game.players, currentHand.currentPlayerId)

        // 7. Get Dummy
        val dummy = game.players.find { it.id == "dummy" }
        dummy ?: throw NotFoundException("Can't find dummy")

        // 8. Validate selected cards
        selectedCards.forEach {
            if (!(me.cards.contains(it) || dummy.cards.contains(it)))
                    throw InvalidOperationException("You can't select this card: $it")
        }

        // 9. Set new hand and remove the dummy
        game.players = game.players.minus(dummy)
        game.players.forEach { if (it.id == playerId) it.cards = selectedCards }
        currentRound.status = RoundStatus.BUYING
        currentHand.currentPlayerId = gameUtils.nextPlayer(game.players, currentRound.dealerId).id
        currentRound.suit = suit

        // 10. Save game
        save(game)

        // 11. Publish updated game
        gameUtils.publishGame(game = game, type = EventType.CHOOSE_FROM_DUMMY)
    }

    @Transactional
    fun buyCards(gameId: String, playerId: String, selectedCards: List<Card>) {
        // 1. Get Game
        val game = get(gameId)

        // 2. Validate number of cards
        gameUtils.validateNumberOfCardsSelectedWhenBuying(selectedCards.size, game.players.size)

        // 3. Get Round
        val currentRound = game.currentRound
        if (currentRound.status != RoundStatus.BUYING)
                throw InvalidOperationException("Can only call in the buying phase")

        // 4. Get current hand
        val currentHand = currentRound.currentHand

        // 5. Validate player
        if (currentHand.currentPlayerId != playerId)
                throw InvalidOperationException("Player ($playerId) is not the current player")

        // 6. Get myself
        val me = gameUtils.findPlayer(game.players, currentHand.currentPlayerId)

        // 7. Validate selected cards
        selectedCards.forEach {
            if (!(me.cards.contains(it)))
                    throw InvalidOperationException("You can't select this card: $it")
        }

        // 8. Get new cards
        val deck = deckService.getDeck(gameId)
        var newCards = selectedCards
        for (x in 0 until 5 - selectedCards.size) newCards =
                newCards.plus(gameUtils.popFromDeck(deck))
        game.players.forEach { player ->
            if (player.id == playerId) {
                player.cards = newCards
                player.cardsBought = 5 - selectedCards.size
            }
        }
        deckService.save(deck)

        // 9. Set next player
        currentHand.currentPlayerId =
                if (currentRound.dealerId == playerId) {
                    currentRound.status = RoundStatus.PLAYING
                    gameUtils.nextPlayer(game.players, currentRound.goerId!!).id
                } else gameUtils.nextPlayer(game.players, me.id).id

        // 10. Save game
        save(game)

        // 11. Publish updated game
        gameUtils.publishGame(game = game, type = EventType.BUY_CARDS, BuyCardsEvent(playerId, 5 - selectedCards.size))
    }

    @Transactional
    fun playCard(gameId: String, playerId: String, myCard: Card) {
        // 1. Get Game
        val game = get(gameId)

        // 2. Get Round
        val currentRound = game.currentRound
        if (currentRound.status != RoundStatus.PLAYING)
                throw InvalidOperationException("Can only call in the playing phase")

        // 3. Get current hand
        val currentHand = currentRound.currentHand
        val suit = currentRound.suit ?: throw InvalidOperationException("No suit selected")

        // 4. Validate player
        if (currentHand.currentPlayerId != playerId)
                throw InvalidOperationException("Player ($playerId) is not the current player")

        // 5. Get myself
        val me = gameUtils.findPlayer(game.players, currentHand.currentPlayerId)

        // 6. Validate selected card
        if (me.cards.isEmpty()) throw InvalidOperationException("You have no cards left")
        if (!(me.cards.contains(myCard)))
                throw InvalidOperationException("You can't select this card: $myCard")

        // 7. Check that they are following suit
        if (currentHand.leadOut == null) currentHand.leadOut = myCard
        else if (!gameUtils.isFollowing(myCard, me.cards, currentHand, suit))
                throw InvalidOperationException("Must follow suit!")

        // 8. Play the card
        game.players.forEach { player ->
            if (player.id == me.id) player.cards = player.cards.minus(myCard)
        }
        currentHand.playedCards = currentHand.playedCards.plus(PlayedCard(me.id, myCard))

        var type = EventType.CARD_PLAYED
        var transitionData: Round? = null

        // 9. Check if current hand is finished
        if (currentHand.playedCards.size < game.players.size) {
            logger.info("Not all players have played a card yet")
            currentHand.currentPlayerId =
                    gameUtils.nextPlayer(game.players, currentHand.currentPlayerId).id
        } else {
            logger.info("All players have played a card")

            if (currentRound.completedHands.size == 3) {
                logger.info(
                        "All hands have been played in this round bar the final hand. We will auto play the final hand."
                )

                // Autoplay final hand
                gameUtils.completeHand(game)
                gameUtils.autoplayLastHand(game)

                // Calculate Scores
                gameUtils.applyScores(game)

                // Set the transition state data
                transitionData = game.currentRound.copy()

                // Check if game is over
                type =
                        if (gameUtils.isGameOver(game.players)) {
                            logger.info("Game is over.")

                            gameUtils.applyWinners(game)

                            game.status = GameStatus.FINISHED
                            EventType.GAME_OVER
                        } else {
                            logger.info("Game isn't over yet. Starting a new round")
                            gameUtils.completeRound(game)
                            EventType.ROUND_COMPLETED
                        }
            } else if (currentRound.completedHands.size < 4) {
                logger.info("Not all hands have been played in this round yet")
                // Create next hand
                transitionData = game.currentRound.copy()
                gameUtils.completeHand(game)
                type = EventType.HAND_COMPLETED
            } else {
                throw Error("Invalid number of rounds detected")
            }
        }

        // 10. Save game
        save(game)

        // 11. Publish updated game
        gameUtils.publishGame(game = game, type = type, transitionData = transitionData)
    }

    companion object {
        private val logger = LogManager.getLogger(GameService::class.java)
        private val VALID_CALL = listOf(0, 10, 15, 20, 25, 30)
        private const val BUNKER_SCORE = -30
    }
}
