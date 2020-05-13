package ie.daithi.cards.service

import ie.daithi.cards.enumeration.Card
import ie.daithi.cards.enumeration.GameStatus
import ie.daithi.cards.enumeration.RoundStatus
import ie.daithi.cards.enumeration.Suit
import ie.daithi.cards.model.Game
import ie.daithi.cards.model.Hand
import ie.daithi.cards.model.Player
import ie.daithi.cards.model.Round
import ie.daithi.cards.repositories.AppUserRepo
import ie.daithi.cards.repositories.GameRepo
import ie.daithi.cards.validation.EmailValidator
import ie.daithi.cards.web.exceptions.InvalidEmailException
import ie.daithi.cards.web.exceptions.InvalidOperationException
import ie.daithi.cards.web.exceptions.InvalidSatusException
import ie.daithi.cards.web.exceptions.NotFoundException
import ie.daithi.cards.web.model.CreatePlayer
import ie.daithi.cards.web.model.enums.PublishContentType
import ie.daithi.cards.web.security.model.AppUser
import ie.daithi.cards.web.security.model.Authority
import org.apache.logging.log4j.LogManager
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.*

@Service
class GameService(
        private val gameRepo: GameRepo,
        private val emailValidator: EmailValidator,
        private val emailService: EmailService,
        private val appUserRepo: AppUserRepo,
        private val passwordEncoder: BCryptPasswordEncoder,
        private val deckService: DeckService,
        private val publishService: PublishService
) {
    fun create(name: String, createPlayers: List<CreatePlayer>, emailMessage: String): Game {
        logger.info("Attempting to start a 110")

        // 1. Put all emails to lower case
        val createPlayersShuffled = createPlayers.shuffled()

        // 2. Validate Emails
        createPlayersShuffled.forEach {
            if (!emailValidator.isValid(it.email))
                throw InvalidEmailException("Invalid email $it")
        }

        // 3. Create Players and Issue emails
        val players = arrayListOf<Player>()
        // If we have six players we will assume this is a team game
        if (createPlayersShuffled.size == 6) {
            val teamId1 = UUID.randomUUID().toString()
            val teamId2 = UUID.randomUUID().toString()
            val teamId3 = UUID.randomUUID().toString()
            players.add(createPlayer(createPlayersShuffled[0], emailMessage, teamId1))
            players.add(createPlayer(createPlayersShuffled[1], emailMessage, teamId2))
            players.add(createPlayer(createPlayersShuffled[2], emailMessage, teamId3))
            players.add(createPlayer(createPlayersShuffled[3], emailMessage, teamId1))
            players.add(createPlayer(createPlayersShuffled[4], emailMessage, teamId2))
            players.add(createPlayer(createPlayersShuffled[5], emailMessage, teamId3))

        } else {
            createPlayersShuffled.forEach {
                // For an individual game set the team ID == email
                players.add(createPlayer(it, emailMessage, it.displayName))
            }
        }

        val timestamp = LocalDateTime.now()

        // 4. Create the first round and assign a dealer
        val dealerId = players[0].id
        val hand = Hand(timestamp = timestamp,
                currentPlayerId = nextPlayer(players, dealerId).id)

        val round = Round(timestamp = timestamp, number = 1, status = RoundStatus.CALLING, currentHand = hand, dealerId = dealerId)

        // 5. Create Game
        var game = Game(
                timestamp = timestamp,
                name = name,
                status = GameStatus.ACTIVE,
                players = players,
                currentRound = round,
                emailMessage = emailMessage)

        game = save(game)

        logger.info("Game started successfully ${game.id}")
        return game
    }

    fun createPlayer(createPlayer: CreatePlayer, emailMessage: String, teamId: String): Player {
        val passwordByte = ByteArray(16)
        secureRandom.nextBytes(passwordByte)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(passwordByte)
        val password = digest.fold("", { str, byt -> str + "%02x".format(byt) })
        val username = createPlayer.displayName.trim()
        val emailAddress = createPlayer.email.toLowerCase().trim()

        val user = AppUser(username = username,
                password = passwordEncoder.encode(password),
                authorities = listOf(Authority.PLAYER))

        appUserRepo.deleteByUsernameIgnoreCase(username)
        appUserRepo.save(user)
        emailService.sendInvite(emailAddress, username, password, emailMessage)

        return Player(id = user.id!!, displayName = user.username!!, teamId = teamId)
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

    fun getActive(): List<Game> {
        return gameRepo.findAllByStatus(GameStatus.ACTIVE)
    }

    fun getActiveByPlayerId(id: String): Game {
        return gameRepo.findByPlayersIdAndStatus(id, GameStatus.ACTIVE)
    }

    fun finish(id: String) {
        val game = get(id)
        if( game.status == GameStatus.ACTIVE) throw InvalidSatusException("Can only finish a game that is in STARTED state not ${game.status}")
        game.status = GameStatus.COMPLETED
        save(game)
    }

    fun cancel(id: String) {
        val game = get(id)
        if( game.status == GameStatus.CANCELLED) throw InvalidSatusException("Game is already in CANCELLED state")
        game.status = GameStatus.CANCELLED
        save(game)
    }

    fun replay(currentGame: Game, playerId: String): Game {

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
                players.add(Player(id = player.id, displayName = player.displayName, teamId = teamIds[3 % index]))
            }

        } else {
            oldPlayers.forEach { player ->
                // For an individual game set the team ID == email
                players.add(Player(id = player.id, displayName = player.displayName, teamId = player.displayName))
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
                players = players,
                currentRound = round,
                emailMessage = currentGame.emailMessage)

        // 6. Save the game
        save(game)

        // 7. Publish updated game
        publishGame(game, playerId)

        // 8. Return the game
        return game
    }

    // Deal a new round
    fun deal(game: Game, playerId: String): Game {

        // 1. Check if they are the dealer
        val dealer = game.currentRound.dealerId
        if (dealer != playerId)
            throw InvalidOperationException("Player $playerId is not the dealer")

        // 2. Order the hands. Dummy second last, dealer last
        game.players = orderPlayersAtStartOfGame(dealer, game.players)

        // 3. Shuffle
        deckService.shuffle(gameId = game.id!!)

        // 4. Deal the cards
        val deck =  deckService.getDeck(game.id)
        for(x in 0 until 5) {
            game.players.forEach {
                it.cards = it.cards.plus(deck.cards.pop())
            }
        }
        deckService.save(deck)

        // 5. Save the game
        save(game)

        // 6. Publish updated game and round
        publishGame(game, playerId)

        // 7. Return the game
        return game
    }

    fun call(gameId: String, playerId: String, call: Int): Game {
        // 1. Validate call value
        if(!VALID_CALL.contains(call)) throw InvalidOperationException("Call value $call is invalid")

        // 2. Get Game
        val game = get(gameId)

        // 3. Get current round
        val currentRound = game.currentRound
        if (currentRound.status != RoundStatus.CALLING) throw InvalidOperationException("Can only call in the calling phase")

        // 4. Get current hand
        val currentHand = currentRound.currentHand

        // 4. Get myself
        val me = findPlayer(game.players, currentHand.currentPlayerId)

        // 5. Check they are the next player
        if (currentHand.currentPlayerId != playerId) throw InvalidOperationException("It's not your go!")

        // 6. Check if call value is valid i.e. > all other calls
        if (call != 0) {
            val currentCaller = game.players.maxBy { it.call } ?: throw Exception("Can't find caller")
            if (currentHand.currentPlayerId == currentRound.dealerId && call >= currentCaller.call) me.call = call
            else if (call > currentCaller.call) me.call = call
            else throw InvalidOperationException("Call must be higher than ${currentCaller.call}")
        }

        // 7. Update my call
        game.players.forEach { if (it.id == me.id) it.call = me.call }

        // 8. Set next player/round status
        if (call == 30) {
            logger.info("Jink called by $me")
            currentRound.status = RoundStatus.CALLED
            currentRound.goerId = me.id
            currentHand.currentPlayerId = me.id
        } else if (currentHand.currentPlayerId == currentRound.dealerId) {

            logger.info("Everyone has called")

            val caller = game.players.maxBy { it.call } ?: throw Exception("This should never happen")

            if (caller.call == 0 && me.call == 0) {
                logger.info("Nobody called anything. Will have to re-deal.")
                completeRound(game)

            } else {
                currentRound.status = RoundStatus.CALLED
                if (me.call == caller.call) {
                    logger.info("Successful call $me")
                    currentRound.goerId = me.id
                    currentHand.currentPlayerId = me.id
                } else {
                    logger.info("Successful call $caller")
                    currentRound.goerId = caller.id
                    currentHand.currentPlayerId = caller.id
                }
            }

        } else {
            logger.info("Still more players to call")
            currentHand.currentPlayerId = nextPlayer(game.players, me.id).id
        }

        // 9. Save game
        save(game)

        // 10. Publish updated game and round
        publishGame(game, me.id)

        return getGameForPlayer(game, me.id)
    }

    fun chooseFromDummy(gameId: String, playerId: String, selectedCards: List<Card>, suit: Suit): Game {
        // 1. Validate number of cards
        if (selectedCards.size < 2) throw InvalidOperationException("You must choose at least 2 cards")
        else if (selectedCards.size > 5) throw InvalidOperationException("You can only choose 5 cards")

        // 2. Get Game
        val game = get(gameId)

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

        // 11. Publish updated game and round
        publishGame(game, me.id)

        return getGameForPlayer(game, me.id)
    }

    fun buyCards(gameId: String, playerId: String, selectedCards: List<Card>): Game {
        // 1. Validate number of cards
        if (selectedCards.size < 2) throw InvalidOperationException("You must choose at least 2 cards")
        else if (selectedCards.size > 5) throw InvalidOperationException("You can only choose 5 cards")

        // 2. Get Game
        val game = get(gameId)

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
        val deck =  deckService.getDeck(game.id!!)
        var newCards = selectedCards
        for(x in 0 until 5 - selectedCards.size) newCards = newCards.plus(deck.cards.pop())
        game.players.forEach { if (it.id == playerId) it.cards = newCards }
        deckService.save(deck)

        // 9. Set next player
        currentHand.currentPlayerId = if (currentRound.dealerId == playerId) {
            currentRound.status = RoundStatus.PLAYING
            nextPlayer(game.players, currentRound.goerId!!).id
        }
        else nextPlayer(game.players, me.id).id

        // 9. Save game
        save(game)

        // 10. Publish updated game and round
        publishGame(game, me.id)

        return getGameForPlayer(game, me.id)
    }

    fun playCard(gameId: String, playerId: String, myCard: Card): Game {
        // 1. Get Game
        val game = get(gameId)
        val timestamp = LocalDateTime.now()

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
        game.players.forEach {
            if (it.id == me.id) {
                it.cards = it.cards.minus(myCard)
            }
        }
        currentHand.playedCards = currentHand.playedCards.plus(Pair(me.id, myCard))

        // 9. Check if current hand is finished
        if (currentHand.playedCards.size < game.players.size) {
            logger.info("Not all players have played a card yet")
            currentHand.currentPlayerId = nextPlayer(game.players, currentHand.currentPlayerId).id
        } else {
            logger.info("All players have played a card")

            if (currentRound.completedHands.size >= 4) {
                logger.info("All hands have been played in this round")

                // Calculate Scores
                calculateScores(game)

                logger.info("Players after calculating score: ${game.players}")

                // Check if game is over
                game.players.forEach {
                    if (it.score >= 110) {
                        game.status = GameStatus.COMPLETED
                    }
                }
                if (game.status == GameStatus.COMPLETED) {
                    logger.info("Game is over.")
                } else {
                    logger.info("Game isn't over yet. Starting a new round")
                    completeRound(game)
                }
            } else {
                logger.info("Not all hands have been played in this round yet")
                // Create next hand
                completeHand(game)
            }
        }


        // 10. Save game
        save(game)

        // 11. Publish updated game and round
        publishGame(game, me.id)

        return getGameForPlayer(game, me.id)
    }

    fun getGameForPlayer(game: Game, playerId: String): Game {
        // TODO Make this work without modifying the object
        // 1. Create a copy of the game object
        // val gameModified = game.copy()
        //        val players = gameModified.players.toMutableList()
        //
        //        players.forEach {
        //            it.cards = it.cards.toMutableList()
        //        }
        //
        //        logger.info("Before: $players")
        //        // 2. Filter out cards of other players
        //        players.forEach { if (!(it.id == playerId || (it.id == "dummy" && playerId == round.goer?.id))) it.cards = emptyList() }
        //        gameModified.players = players
        //
        // logger.info("After: $players")
        // return gameModified
        return game
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
        return game
    }

    private fun publishGame(game: Game, callerId: String) {

        game.players.forEach {
            if (it.id != callerId && it.id != "dummy") {
                publishService.publishContent(recipient = it.displayName,
                        topic = "/game",
                        content = getGameForPlayer(game = game, playerId = it.id),
                        gameId = game.id!!,
                        contentType = PublishContentType.GAME)
            }
        }
    }

    private fun calculateScores(game: Game): List<Player> {
        // 1. Find winner of each hand
        var bestCard: Pair<Player, Card>? = null
        val scores: MutableMap<String, Int> = mutableMapOf()

        val currentRound = game.currentRound

        currentRound.completedHands.plus(currentRound.currentHand).forEach {
            val winner = handWinner(it, currentRound.suit!!, game.players)
            val currentScore = scores[winner.first.teamId]
            val suit = currentRound.suit ?: throw InvalidOperationException("No suit detected")
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

        logger.debug("Scores after best card: $scores")

        // 3. Check if the goer made their contract and assign their points
        val goerId = currentRound.goerId ?: throw InvalidOperationException("No goer set")
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

        return game.players
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
            currentHand.playedCards[players[0].id]?.suit
        else suit

        logger.info("Active suit is: $activeSuit")

        // 2. Find winning card
        val winningCard = if (activeSuit == suit) currentHand.playedCards.filter { it.value.suit == activeSuit || it.value.suit == Suit.WILD }.maxBy { it.value.value }
        else currentHand.playedCards.filter { it.value.suit == activeSuit }.maxBy { it.value.coldValue }
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

    private fun previousPlayer(players: List<Player>, currentPlayerId: String): Player {
        val currentIndex = players.indexOfFirst { it.id == currentPlayerId }
        if (currentIndex == 0) return players.last()
        return players[currentIndex + 1]
    }

    private fun orderPlayersAtStartOfGame(dealerId: String, players: List<Player>): List<Player> {
        val dummy = players.find { it.id == "dummy" }
        val playersSansDummy = if (dummy != null) players.minus(dummy)
        else players
        val dealer = players.find { it.id == dealerId }
        val playerStack = Stack<Player>()
        playerStack.push(dealer)
        playerStack.push(Player("dummy","dummy", teamId = "dummy"))
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

    companion object {
        private val logger = LogManager.getLogger(this::class.java)
        private val secureRandom = SecureRandom()
        private val VALID_CALL = listOf(0, 10, 15, 20, 25, 30)
    }

}