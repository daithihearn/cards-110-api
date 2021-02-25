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
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class GameService(
        private val gameRepo: GameRepo,
        private val deckService: DeckService,
        private val publishService: PublishService
) {
    @Transactional
    fun create(adminId: String, name: String, playerIds: List<String>): Game {
        logger.info("Attempting to start a game of 110")

        // 1. Validate number of players
        if (playerIds.size !in 2..6) throw InvalidOperationException("Please add 2-6 players")

        // 2. Shuffle players
        val playerIdsShuffled = playerIds.shuffled()

        // 4. Create Players and Issue emails
        val players = arrayListOf<Player>()
        // If we have six players we will assume this is a team game
        if (playerIdsShuffled.size == 6) {
            val teamIds = listOf(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString())

            players.add(Player(id = playerIdsShuffled[0], seatNumber = 1, teamId = teamIds[0]))
            players.add(Player(id = playerIdsShuffled[1], seatNumber = 2, teamId = teamIds[1]))
            players.add(Player(id = playerIdsShuffled[2], seatNumber = 3, teamId = teamIds[2]))
            players.add(Player(id = playerIdsShuffled[3], seatNumber = 4, teamId = teamIds[0]))
            players.add(Player(id = playerIdsShuffled[4], seatNumber = 5, teamId = teamIds[1]))
            players.add(Player(id = playerIdsShuffled[5], seatNumber = 6, teamId = teamIds[2]))

        } else {
            playerIdsShuffled.forEachIndexed { index, playerId -> players.add(Player(id = playerId, seatNumber = index + 1, teamId = playerId)) }
        }

        // 5. Create the first round and assign a dealer
        val timestamp = LocalDateTime.now()
        val dealerId = players[0].id
        val hand = Hand(timestamp = timestamp,
                currentPlayerId = nextPlayer(players, dealerId).id)

        val round = Round(timestamp = timestamp, number = 1, status = RoundStatus.CALLING, currentHand = hand, dealerId = dealerId)

        // 6. Create Game
        var game = Game(
                timestamp = timestamp,
                name = name,
                status = GameStatus.ACTIVE,
                adminId = adminId,
                players = players,
                currentRound = round)

        game = save(game)

        logger.info("Game started successfully ${game.id}")
        return game
    }

    fun get(id: String): Game {
        val game = gameRepo.findById(id)
        if (!game.isPresent)
            throw NotFoundException("Game $id not found")
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

    fun getActiveForPlayer(playerId: String): List<Game> {
        return gameRepo.findByPlayersIdAndStatusOrStatus(playerId, GameStatus.ACTIVE, GameStatus.FINISHED)
    }

    fun getActiveForAdmin(adminId: String): List<Game> {
        return gameRepo.findByAdminIdAndStatusOrStatus(adminId, GameStatus.ACTIVE, GameStatus.FINISHED)
    }

    @Transactional
    fun finish(id: String) {
        val game = get(id)
        if( game.status == GameStatus.ACTIVE) throw InvalidStatusException("Can only finish a game that is in STARTED state not ${game.status}")
        game.status = GameStatus.COMPLETED
        save(game)
    }

    @Transactional
    fun cancel(id: String) {
        val game = get(id)
        if( game.status == GameStatus.CANCELLED) throw InvalidStatusException("Game is already in CANCELLED state")
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
            val teamIds = listOf(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString())
            oldPlayers.forEachIndexed { index, player ->
                players.add(Player(id = player.id, seatNumber = index + 1, teamId = teamIds[3 % index]))
            }

        } else {
            oldPlayers.forEachIndexed { index, player ->
                // For an individual game set the team ID == email
                players.add(Player(id = player.id, seatNumber = index + 1, teamId = player.id))
            }
        }

        // 4. Create the first round and assign a dealer
        val dealerId = players[0].id
        val hand = Hand(timestamp = timestamp,
                currentPlayerId = nextPlayer(players, dealerId).id)

        val round = Round(timestamp = timestamp, number = 1, status = RoundStatus.CALLING, currentHand = hand, dealerId = dealerId)

        // 5. Create Game
        var game = Game(
                timestamp = timestamp,
                name = currentGame.name,
                status = GameStatus.ACTIVE,
                adminId = currentGame.adminId,
                players = players,
                currentRound = round,
                emailMessage = currentGame.emailMessage)

        // 6. Save the game
        game = save(game)

        // 7. Publish updated game
        publishGame(game = game, type = EventType.REPLAY)
    }

    @Transactional
    // Deal a new round
    fun deal(game: Game, playerId: String) {

        // 1. Check if they are the dealer
        val dealer = game.currentRound.dealerId
        if (dealer != playerId)
            throw InvalidOperationException("Player $playerId is not the dealer")

        // 2. Check if can deal
        if (game.players.first().cards.isNotEmpty())
            throw InvalidOperationException("Cards have already been dealt")


        // 3. Order the hands. Dummy second last, dealer last
        game.players = orderPlayersAtStartOfGame(dealer, game.players)

        // 4. Shuffle
        deckService.shuffle(gameId = game.id!!)

        // 5. Deal the cards
        val deck =  deckService.getDeck(game.id)
        for(x in 0 until 5) {
            game.players.forEach { player -> player.cards = player.cards.plus(popFromDeck(deck)) }
        }
        deckService.save(deck)

        // 6. Reset bought cards
        game.players.forEach { player -> player.cardsBought = null }

        // 7. Save the game
        save(game)

        // 8. Publish updated game
        publishGame(game = game, type = EventType.DEAL)
    }

    private fun popFromDeck(deck: Deck): Card {
        if (deck.cards.empty()) throw NotFoundException("The deck(${deck.id}) is empty. This should never happen.")
        return deck.cards.pop()
    }

    @Transactional
    fun call(gameId: String, playerId: String, call: Int) {
        // 1. Validate call value
        if(!VALID_CALL.contains(call)) throw InvalidOperationException("Call value $call is invalid")

        // 2. Get Game
        val game = get(gameId)

        // 3. Get current round
        val currentRound = game.currentRound
        if (currentRound.status != RoundStatus.CALLING) throw InvalidOperationException("Can only call in the calling phase")

        // 4. Get current hand
        val currentHand = currentRound.currentHand

        // 5. Get myself
        val me = findPlayer(game.players, currentHand.currentPlayerId)

        // 6. Check they are the next player
        if (currentHand.currentPlayerId != playerId) throw InvalidOperationException("It's not your go!")

        // 7. Check if call value is valid i.e. > all other calls
        if  (currentRound.dealerSeeingCall) {
            me.call = call
        } else if (call != 0) {
            val currentCaller = game.players.maxByOrNull { it.call } ?: throw Exception("Can't find caller")
            if (currentHand.currentPlayerId == currentRound.dealerId && call >= currentCaller.call) me.call = call
            else if (call > currentCaller.call) me.call = call
            else throw InvalidOperationException("Call must be higher than ${currentCaller.call}")
        }

        // 8. Update my call
        game.players.forEach { if (it.id == me.id) it.call = me.call }

        // 9. Set next player/round status
        var type = EventType.CALL
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

            val caller = game.players.maxByOrNull { it.call } ?: throw Exception("This should never happen")

            if (caller.call == 0 && me.call == 0) {
                logger.info("Nobody called anything. Will have to re-deal.")
                completeRound(game)
                type = EventType.ROUND_COMPLETED
            } else if (me.call == caller.call && me.id != caller.id) {
                logger.info("Dealer saw a call. Go back to caller.")
                currentHand.currentPlayerId = caller.id
                currentRound.dealerSeeingCall = true
            } else if (caller.call == 10) {
                logger.info("Can't go 10")
                completeRound(game)
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
                val caller = game.players.maxByOrNull { it.call } ?: throw Exception("This should never happen")
                if (caller.id != me.id) throw InvalidOperationException("Invalid call")
                logger.info("${me.id} has raised the call")
                currentHand.currentPlayerId = currentRound.dealerId
                currentRound.dealerSeeingCall = false
            }
        } else {
            logger.info("Still more players to call")
            currentHand.currentPlayerId = nextPlayer(game.players, me.id).id
        }

        // 10. Save game
        save(game)

        // 11. Publish updated game
        publishGame(game = game, type = type)
    }

    @Transactional
    fun chooseFromDummy(gameId: String, playerId: String, selectedCards: List<Card>, suit: Suit) {
        // 1. Get Game
        val game = get(gameId)

        // 2. Validate number of cards (-1 because of the dummy)
        validateNumberOfCardsSelectedWhenBuying(selectedCards.size, game.players.size - 1)

        // 3. Get Round
        val currentRound = game.currentRound
        if (currentRound.status != RoundStatus.CALLED) throw InvalidOperationException("Can only call in the called phase")

        // 4. Get current hand
        val currentHand = currentRound.currentHand

        // 5. Validate player
        if(currentRound.goerId != playerId || currentHand.currentPlayerId != playerId)
            throw InvalidOperationException("Player ($playerId) is not the goer and current player")

        // 6. Get myself
        val me = findPlayer(game.players, currentHand.currentPlayerId)

        // 7. Get Dummy
        val dummy = game.players.find { it.id == "dummy" }
        dummy?: throw NotFoundException("Can't find dummy")

        // 8. Validate selected cards
        selectedCards.forEach {
            if (!(me.cards.contains(it) || dummy.cards.contains(it)))
                throw InvalidOperationException("You can't select this card: $it")
        }

        // 9. Set new hand and remove the dummy
        game.players = game.players.minus(dummy)
        game.players.forEach { if (it.id == playerId) it.cards = selectedCards }
        currentRound.status = RoundStatus.BUYING
        currentHand.currentPlayerId = nextPlayer(game.players, currentRound.dealerId).id
        currentRound.suit = suit

        // 10. Save game
        save(game)

        // 11. Publish updated game
        publishGame(game = game, type = EventType.CHOOSE_FROM_DUMMY)
    }

    @Transactional
    fun buyCards(gameId: String, playerId: String, selectedCards: List<Card>) {
        // 1. Get Game
        val game = get(gameId)

        // 2. Validate number of cards
        validateNumberOfCardsSelectedWhenBuying(selectedCards.size, game.players.size)

        // 3. Get Round
        val currentRound = game.currentRound
        if (currentRound.status != RoundStatus.BUYING) throw InvalidOperationException("Can only call in the buying phase")

        // 4. Get current hand
        val currentHand = currentRound.currentHand

        // 5. Validate player
        if(currentHand.currentPlayerId != playerId)
            throw InvalidOperationException("Player ($playerId) is not the current player")

        // 6. Get myself
        val me = findPlayer(game.players, currentHand.currentPlayerId)

        // 7. Validate selected cards
        selectedCards.forEach {
            if (!(me.cards.contains(it)))
                throw InvalidOperationException("You can't select this card: $it")
        }

        // 8. Get new cards
        val deck = deckService.getDeck(gameId)
        var newCards = selectedCards
        for(x in 0 until 5 - selectedCards.size) newCards = newCards.plus(popFromDeck(deck))
        game.players.forEach { player -> if (player.id == playerId) {
            player.cards = newCards
            player.cardsBought = 5 - selectedCards.size
        } }
        deckService.save(deck)

        // 9. Set next player
        currentHand.currentPlayerId = if (currentRound.dealerId == playerId) {
            currentRound.status = RoundStatus.PLAYING
            nextPlayer(game.players, currentRound.goerId!!).id
        }
        else nextPlayer(game.players, me.id).id

        // 10. Save game
        save(game)

        // 11. Publish updated game
        publishGame(game = game, type = EventType.BUY_CARDS)

        // 12. Publish buy cards event
        publishBuyCardsEvent(game, BuyCardsEvent(playerId, 5 - selectedCards.size))
    }

    @Transactional
    fun playCard(gameId: String, playerId: String, myCard: Card) {
        // 1. Get Game
        val game = get(gameId)

        // 2. Get Round
        val currentRound = game.currentRound
        if (currentRound.status != RoundStatus.PLAYING) throw InvalidOperationException("Can only call in the playing phase")

        // 3. Get current hand
        val currentHand = currentRound.currentHand
        val suit = currentRound.suit ?: throw InvalidOperationException("No suit selected")

        // 4. Validate player
        if(currentHand.currentPlayerId != playerId)
            throw InvalidOperationException("Player ($playerId) is not the current player")

        // 5. Get myself
        val me = findPlayer(game.players, currentHand.currentPlayerId)

        // 6. Validate selected card
        if (me.cards.isEmpty()) throw InvalidOperationException("You have no cards left")
        if (!(me.cards.contains(myCard)))
            throw InvalidOperationException("You can't select this card: $myCard")

        // 7. Check that they are following suit
        if (currentHand.leadOut == null)
            currentHand.leadOut = myCard
        else if (!isFollowing(myCard, me.cards, currentHand, suit))
            throw InvalidOperationException("Must follow suit!")

        // 8. Play the card
        game.players.forEach { player ->
            if (player.id == me.id) player.cards = player.cards.minus(myCard)
        }
        currentHand.playedCards = currentHand.playedCards.plus(Pair(me.id, myCard))

        var type = EventType.CARD_PLAYED

        // 9. Check if current hand is finished
        if (currentHand.playedCards.size < game.players.size) {
            logger.info("Not all players have played a card yet")
            currentHand.currentPlayerId = nextPlayer(game.players, currentHand.currentPlayerId).id
        } else {
            logger.info("All players have played a card")

            // Publish the game and wait 4 seconds. This is to allow time to see the card
            // TODO Use a computable future or something rather than stopping the thread
            publishGame(game = game, type = EventType.LAST_CARD_PLAYED)
            Thread.sleep(4000)

            if (currentRound.completedHands.size >= 4) {
                logger.info("All hands have been played in this round")

                // Calculate Scores
                calculateScores(game)

                // Check if game is over
                type = if (isGameOver(game.players)) {
                    logger.info("Game is over.")
                    game.status = GameStatus.FINISHED
                    EventType.GAME_OVER
                } else {
                    logger.info("Game isn't over yet. Starting a new round")
                    completeRound(game)
                    EventType.ROUND_COMPLETED
                }
            } else {
                logger.info("Not all hands have been played in this round yet")
                // Create next hand
                completeHand(game)
                type = EventType.HAND_COMPLETED
            }
        }


        // 10. Save game
        save(game)

        // 11. Publish updated game
        publishGame(game = game, type = type)
    }

    fun isGameOver(players: List<Player>): Boolean {
        return players.maxOf { player -> player.score } >= 110
    }

    fun parsePlayerGameState(game: Game, playerId: String): PlayerGameState {

        // 1. Find player
        val me = game.players.find { player -> player.id == playerId } ?: throw NotFoundException("Can't find player")

        // 2. Find dummy
        val dummy = (game.currentRound.goerId == playerId).let {
            game.players.find { player -> player.id == "dummy" }
        }

        // 3. Get max call
        val highestCaller = game.players.maxByOrNull { player -> player.call }

        // 5. Return player's game state
        return PlayerGameState(
                me = me,
                cards = me.cards,
                status = game.status,
                dummy = dummy?.cards,
                round = game.currentRound,
                maxCall = highestCaller?.call ?: 0,
                playerProfiles = game.players.filter { p -> p.id != "dummy" }
        )
    }

    private fun completeHand(game: Game): Game {

        val suit = game.currentRound.suit ?: throw InvalidOperationException("Suit not set")

        // 1. Find hand winner
        val handWinner = handWinner(game.currentRound.currentHand, suit, game.players)

        // 2. Add hand to completed
        game.currentRound.completedHands = game.currentRound.completedHands.plus(game.currentRound.currentHand)

        // 3. Create the next hand
        game.currentRound.currentHand = Hand(timestamp = LocalDateTime.now(),
                currentPlayerId = handWinner.first.id)

        // 4. Reorder the players
        game.players = orderPlayers(handWinner.first, game.players)

        return game
    }

    private fun completeRound(game: Game): Game {
        val timestamp = LocalDateTime.now()

        // 1. Add round to completed
        game.completedRounds = game.completedRounds.plus(game.currentRound)

        // 2. Create next round
        val nextDealerId = nextPlayer(game.players, game.currentRound.dealerId).id
        val nextHand = Hand(timestamp = timestamp,
                currentPlayerId = nextPlayer(game.players, nextDealerId).id)

        game.currentRound = Round(timestamp = timestamp,
                number = game.currentRound.number + 1,
                dealerId = nextDealerId,
                currentHand = nextHand, status = RoundStatus.CALLING)

        // 3. Clear cards
        game.players.forEach { player ->
            player.cards = emptyList()
        }

        return game
    }

    private fun publishGame(game: Game, type: EventType) {

        game.players.forEach { player ->
            if (player.id != "dummy") {
                publishService.publishContent(recipient = "${player.id}${game.id!!}",
                        topic = "/game",
                        content = parsePlayerGameState(game = game, playerId = player.id),
                        contentType = type)
            }
        }
    }

    private fun publishBuyCardsEvent(game: Game, buyCardsEvent: BuyCardsEvent) {

        game.players.forEach { player ->
            if (player.id != "dummy" && player.id != buyCardsEvent.playerId) {
                publishService.publishContent(recipient = "${player.id}${game.id!!}",
                        topic = "/game",
                        content = buyCardsEvent,
                        contentType = EventType.BUY_CARDS_NOTIFICATION)
            }
        }
    }

    private fun calculateScores(game: Game): List<Player> {
        // 1. Calculate the scores for the round
        val scores = calculateScoresForRound(game.currentRound, game.players)

        // 2. Apply the calculated scores
        applyScoresForRound(game, scores)

        return game.players
    }

    fun calculateScoresForRound(round: Round, players: List<Player>): MutableMap<String, Int> {

        var bestCard: Pair<Player, Card>? = null
        val scores: MutableMap<String, Int> = mutableMapOf()

        // 1. Calculate winners
        round.completedHands.plus(round.currentHand).forEach {
            val winner = handWinner(it, round.suit!!, players)
            val currentScore = scores[winner.first.teamId]
            val suit = round.suit ?: throw InvalidOperationException("No suit detected")
            if (currentScore == null) scores[winner.first.teamId] = 5
            else scores[winner.first.teamId] = currentScore + 5

            bestCard = if (bestCard == null) winner
            // Both Trumps
            else if (isTrump(suit, bestCard!!.second) && isTrump(suit, winner.second)
                    && winner.second.value > bestCard!!.second.value) winner
            // Both Cold
            else if (notTrump(suit, bestCard!!.second) && notTrump(suit, winner.second)
                    && winner.second.coldValue > bestCard!!.second.coldValue) winner
            // Only winner is trump
            else if (notTrump(suit, bestCard!!.second) && isTrump(suit, winner.second)) winner
            // Only bestCard is trump
            else bestCard
        }
        logger.debug("Scores before best card: $scores")
        bestCard ?: throw InvalidOperationException("No best card identified")
        logger.info("Best card played this round: $bestCard")

        // 2. Add points for best card
        val currentScore = scores[bestCard!!.first.teamId]
        if (currentScore == null) scores[bestCard!!.first.teamId] = 5
        else scores[bestCard!!.first.teamId] = currentScore + 5

        return scores
    }

    private fun applyScoresForRound(game: Game, scores: MutableMap<String, Int>): Game {
        val goerId = game.currentRound.goerId ?: throw InvalidOperationException("No goer set")
        val goer = findPlayer(game.players, goerId)
        val goerScore = scores[goer.teamId] ?: 0
        if (goerScore >= goer.call) {
            logger.info("Team ${goer.teamId} made the contract of ${goer.call} with a score of $goerScore")
            if (goer.call == 30) {
                logger.info("Successful Jink called!!")
                updatePlayersScore(game.players, goer.teamId, goerScore * 2)
            } else {
                updatePlayersScore(game.players, goer.teamId, goerScore)
            }
            scores.remove(goer.teamId)
        } else {
            logger.info("Team ${goer.teamId} did not make the contract of ${goer.call} with a score of $goerScore")
            updatePlayersScore(game.players, goer.teamId, -goer.call)
            scores.remove(goer.teamId)
        }

        scores.forEach {
            updatePlayersScore(players = game.players, teamId = it.key, points = it.value)
        }
        return game
    }

    private fun findPlayer(players: List<Player>, playerId: String): Player {
        return players.find { it.id == playerId } ?: throw NotFoundException("Can't find the player")
    }

    private fun isTrump(suit: Suit, card: Card): Boolean {
        return card.suit == suit || card.suit == Suit.WILD
    }

    private fun notTrump(suit: Suit, card: Card): Boolean {
        return !isTrump(suit, card)
    }


    private fun updatePlayersScore(players: List<Player>, teamId: String, points: Int) {
        players.forEach {
            if(it.teamId == teamId) it.score = it.score + points
        }
    }

    private fun handWinner(currentHand: Hand, suit: Suit, players: List<Player>): Pair<Player, Card> {

        // 1. Was a suit or wild card played? If not set the lead out card as the suit
        val activeSuit  = if (currentHand.playedCards.filter { it.value.suit == suit || it.value.suit == Suit.WILD }.isEmpty())
            currentHand.leadOut!!.suit
        else suit

        logger.info("Active suit is: $activeSuit")

        // 2. Find winning card
        val winningCard = if (activeSuit == suit) currentHand.playedCards.filter { it.value.suit == activeSuit || it.value.suit == Suit.WILD }.maxByOrNull { it.value.value }
        else currentHand.playedCards.filter { it.value.suit == activeSuit }.maxByOrNull { it.value.coldValue }
        winningCard?: throw InvalidOperationException("Can't find the winning card")

        logger.info("Winning card is: $winningCard")

        val winner = players.find { it.id == winningCard.key } ?: throw NotFoundException("Couldn't find winning player")

        logger.info("Winner is: $winner")

        return Pair(winner, winningCard.value)
    }

    private fun isFollowing(myCard: Card, myCards: List<Card>, currentHand: Hand, suit: Suit): Boolean {
        val suitLead = currentHand.leadOut!!.suit == suit || currentHand.leadOut!!.suit == Suit.WILD

        if (suitLead) {
            val myTrumps = myCards.filter { it.suit == suit || it.suit == Suit.WILD }
            return myTrumps.isEmpty() || myCard.suit == suit || myCard.suit == Suit.WILD || canRenage(currentHand.leadOut!!, myTrumps)
        }

        val mySuitedCards = myCards.filter { it.suit == currentHand.leadOut!!.suit }
        return mySuitedCards.isEmpty() || myCard.suit == suit || myCard.suit == Suit.WILD || myCard.suit == currentHand.leadOut!!.suit
    }

    private fun canRenage(leadOut: Card, myTrumps: List<Card>): Boolean {
        myTrumps.forEach {
            if (!it.renegable || it.value <= leadOut.value)
                return false
        }
        return true
    }

    private fun nextPlayer(players: List<Player>, currentPlayerId: String): Player {

        val currentIndex = players.indexOfFirst { it.id == currentPlayerId }
        if (currentIndex == -1) throw NotFoundException("Can't find player $currentPlayerId")
        if (currentIndex < players.size - 1) {
            if (players[currentIndex + 1].id == "dummy")
                return nextPlayer(players, players[currentIndex + 1].id)
            return players[currentIndex + 1]
        }
        return players[0]
    }

    private fun orderPlayersAtStartOfGame(dealerId: String, players: List<Player>): List<Player> {
        val dummy = players.find { it.id == "dummy" }
        val playersSansDummy = if (dummy != null) players.minus(dummy)
        else players
        val dealer = players.find { it.id == dealerId }
        val playerStack = Stack<Player>()
        playerStack.push(dealer)
        playerStack.push(Player(id = "dummy", seatNumber = 0, teamId = "dummy"))
        var currentIndex = playersSansDummy.indexOf(dealer)
        for(x in 0 until playersSansDummy.size - 1) {
            currentIndex = if(currentIndex < 1)
                playersSansDummy.size - 1
            else currentIndex - 1
            playerStack.push(playersSansDummy[currentIndex])
        }

        // Clear all calls and cards
        players.forEach {
            it.cards = emptyList()
            it.call = 0
        }

        return playerStack.toMutableList().reversed()
    }

    private fun orderPlayers(currentPlayer: Player, players: List<Player>): List<Player> {
        val dummy = players.find { it.id == "dummy" }
        val playersSansDummy = if (dummy != null) players.minus(dummy)
        else players
        val playerStack = Stack<Player>()
        playerStack.push(currentPlayer)
        var currentIndex = playersSansDummy.indexOf(currentPlayer)
        for(x in 0 until playersSansDummy.size - 1) {
            currentIndex = if(currentIndex >= playersSansDummy.size - 1)
                0
            else currentIndex + 1
            playerStack.push(playersSansDummy[currentIndex])
        }
        return playerStack.toMutableList()
    }

    private fun validateNumberOfCardsSelectedWhenBuying(cardsSelected: Int, numPlayers: Int) {
        val numberHaveToKeep = when (numPlayers) {
            in 2..4  -> 0
            5 -> 1
            else -> 2
        }

        if (cardsSelected < numberHaveToKeep) throw InvalidOperationException("You must choose at least $numberHaveToKeep cards")
        else if (cardsSelected > 5) throw InvalidOperationException("You can only choose 5 cards")
    }

    companion object {
        private val logger = LogManager.getLogger(GameService::class.java)
        private val VALID_CALL = listOf(0, 10, 15, 20, 25, 30)
    }

}