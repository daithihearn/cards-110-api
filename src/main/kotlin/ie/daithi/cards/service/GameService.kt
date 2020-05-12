package ie.daithi.cards.service

import ie.daithi.cards.enumeration.Card
import ie.daithi.cards.enumeration.GameStatus
import ie.daithi.cards.enumeration.RoundStatus
import ie.daithi.cards.enumeration.Suit
import ie.daithi.cards.model.Game
import ie.daithi.cards.model.Player
import ie.daithi.cards.model.Round
import ie.daithi.cards.repositories.AppUserRepo
import ie.daithi.cards.repositories.GameRepo
import ie.daithi.cards.validation.EmailValidator
import ie.daithi.cards.web.exceptions.InvalidEmailException
import ie.daithi.cards.web.exceptions.InvalidOperationException
import ie.daithi.cards.web.exceptions.InvalidSatusException
import ie.daithi.cards.web.exceptions.NotFoundException
import ie.daithi.cards.web.model.enums.PublishContentType
import ie.daithi.cards.web.security.model.AppUser
import ie.daithi.cards.web.security.model.Authority
import org.apache.logging.log4j.LogManager
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

@Service
class GameService(
        private val gameRepo: GameRepo,
        private val emailValidator: EmailValidator,
        private val emailService: EmailService,
        private val appUserRepo: AppUserRepo,
        private val passwordEncoder: BCryptPasswordEncoder,
        private val deckService: DeckService,
        private val roundService: RoundService,
        private val publishService: PublishService
) {
    fun create(name: String, playerEmails: List<String>, emailMessage: String): Game {
        logger.info("Attempting to start a 110")

        // 1. Put all emails to lower case
        val lowerCaseEmails = playerEmails.map(String::toLowerCase)

        // 2. Validate Emails
        lowerCaseEmails.forEach {
            if (!emailValidator.isValid(it))
                throw InvalidEmailException("Invalid email $it")
        }

        // 3. Create Players and Issue emails
        val players = arrayListOf<Player>()
        // If we have six players we will assume this is a team game
        if (lowerCaseEmails.size == 6) {
            val teamId1 = UUID.randomUUID().toString()
            val teamId2 = UUID.randomUUID().toString()
            val teamId3 = UUID.randomUUID().toString()
            players.add(createPlayer(lowerCaseEmails[0], emailMessage, teamId1))
            players.add(createPlayer(lowerCaseEmails[1], emailMessage, teamId2))
            players.add(createPlayer(lowerCaseEmails[2], emailMessage, teamId3))
            players.add(createPlayer(lowerCaseEmails[3], emailMessage, teamId1))
            players.add(createPlayer(lowerCaseEmails[4], emailMessage, teamId2))
            players.add(createPlayer(lowerCaseEmails[5], emailMessage, teamId3))

        } else {
            lowerCaseEmails.forEach {
                // For an individual game set the team ID == email
                players.add(createPlayer(it, emailMessage, it))
            }
        }

        // 4. Create Game
        var game = Game(
                status = GameStatus.ACTIVE,
                name = name,
                players = players,
                emailMessage = emailMessage)

        game = save(game)

        logger.info("Game started successfully ${game.id}")
        return game
    }

    fun createPlayer(username: String, emailMessage: String, teamId: String): Player {
        val passwordByte = ByteArray(16)
        secureRandom.nextBytes(passwordByte)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(passwordByte)
        val password = digest.fold("", { str, byt -> str + "%02x".format(byt) })

        val user = AppUser(username = username,
                password = passwordEncoder.encode(password),
                authorities = listOf(Authority.PLAYER))

        appUserRepo.deleteByUsernameIgnoreCase(username)
        appUserRepo.save(user)
        emailService.sendInvite(username, password, emailMessage)

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

    fun getByPlayerId(id: String): Game {
        return gameRepo.getByPlayerId(id)
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

    // Deal a new round
    fun deal(game: Game, dealerOpt: Player?): Pair<Game, Round> {

        val dealer = dealerOpt ?: game.players[0]

        // 1. Shuffle
        deckService.shuffle(gameId = game.id!!)

        // 2. Order the hands. Dummy second last, dealer last
        game.players = orderPlayersAtStartOfGame(dealer, game.players)

        // 3. Clear cards
        game.players.forEach {
            it.cards = emptyList()
            it.call = 0
        }

        // 4. Deal the cards
        val deck =  deckService.getDeck(game.id)
        for(x in 0 until 5) {
            game.players.forEach {
                it.cards = it.cards.plus(deck.cards.pop())
            }
        }
        deckService.save(deck)

        // 5. Store the round
        val round = Round(id = game.id,
                dealer = dealer,
                status = RoundStatus.CALLING,
                currentPlayer = game.players[0])

        // 6. Return the round
        return Pair(game, round)
    }

    fun call(gameId: String, playerId: String, call: Int): Pair<Game, Round> {
        // 1. Validate call value
        if(!VALID_CALL.contains(call)) throw InvalidOperationException("Call value $call is invalid")

        // 2. Get Game
        var game = get(gameId)

        // 3. Get Round
        var round = roundService.get(game.id!!)
        if (round.status != RoundStatus.CALLING) throw InvalidOperationException("Can only call in the calling phase")

        // 4. Find myself
        val me = game.players.find { it.id == round.currentPlayer.id }
        me?: throw NotFoundException("Can't find current player")

        // 5. Check they are the next player
        if (round.currentPlayer.id != playerId) throw InvalidOperationException("It's not your go!")

        // 6. Check if call value is valid i.e. > all other calls
        if (call != 0) {
            val currentCaller = game.players.maxBy { it.call }
            currentCaller ?: throw Exception("Can't find caller")
            if (round.currentPlayer == round.dealer && call >= currentCaller.call) me.call = call
            else if (call > currentCaller.call) me.call = call
            else throw InvalidOperationException("Call must be higher than ${currentCaller.call}")
        }

        // 7. Update my call
        game.players.forEach { if (it.id == me.id) it.call = me.call }

        // 8. Set next player/round status
        if (call == 30) {
            round.status = RoundStatus.CALLED
            round.goer = me
            round.currentPlayer = me
        } else if (round.currentPlayer.id == round.dealer.id) {
            val caller = game.players.maxBy { it.call }
            caller ?: throw Exception("This should never happen")
            if (caller.call == 0 && me.call == 0) {

                val gameAndRound = deal(game, nextPlayer(game.players, round.dealer))
                game = gameAndRound.first
                round = gameAndRound.second
            } else {
                round.status = RoundStatus.CALLED
                if (me.call == caller.call) {
                    round.goer = me
                    round.currentPlayer = me
                } else {
                    round.goer = caller
                    round.currentPlayer = caller
                }
            }

        } else {
            round.currentPlayer = nextPlayer(game.players, me)
        }

        // 9. Save round
        roundService.save(round)
        save(game)

        // 10. Publish updated game and round
        publishGameAndRound(game, round, me)

        return Pair(getGameForPlayer(game, round, me.id), round)
    }

    fun chooseFromDummy(gameId: String, playerId: String, selectedCards: List<Card>, suit: Suit): Pair<Game, Round> {
        // 1. Validate number of cards
        if (selectedCards.size < 2) throw InvalidOperationException("You must choose at least 2 cards")
        else if (selectedCards.size > 5) throw InvalidOperationException("You can only choose 5 cards")

        // 2. Get Game
        val game = get(gameId)

        // 3. Get Round
        val round = roundService.get(game.id!!)
        if (round.status != RoundStatus.CALLED) throw InvalidOperationException("Can only call in the called phase")

        // 4. Validate player
        if(round.goer!!.id != playerId || round.currentPlayer.id != playerId)
            throw InvalidOperationException("Player ($playerId) is not the goer and current player")

        // 5. Get my hand
        val me = game.players.find { it.id == round.currentPlayer.id }
        me?: throw NotFoundException("Can't find current player")

        // 6. Get Dummy
        val dummy = game.players.find { it.id == "dummy" }
        dummy?: throw NotFoundException("Can't find dummy")

        // 5. Validate selected cards
        selectedCards.forEach {
            if (!(me.cards.contains(it) || dummy.cards.contains(it)))
                throw InvalidOperationException("You can't select this card: $it")
        }

        // 6. Set new hand and remove the dummy
        game.players = game.players.minus(dummy)
        game.players.forEach { if (it.id == playerId) it.cards = selectedCards }
        round.status = RoundStatus.BUYING
        round.currentPlayer = nextPlayer(game.players, round.dealer)
        round.suit = suit

        // 7. Save round
        roundService.save(round)
        save(game)

        // 8. Publish updated game and round
        publishGameAndRound(game, round, me)

        return Pair(getGameForPlayer(game, round, me.id), round)
    }

    fun buyCards(gameId: String, playerId: String, selectedCards: List<Card>): Pair<Game, Round> {
        // 1. Validate number of cards
        if (selectedCards.size < 2) throw InvalidOperationException("You must choose at least 2 cards")
        else if (selectedCards.size > 5) throw InvalidOperationException("You can only choose 5 cards")

        // 2. Get Game
        val game = get(gameId)

        // 3. Get Round
        val round = roundService.get(game.id!!)
        if (round.status != RoundStatus.BUYING) throw InvalidOperationException("Can only call in the buying phase")

        // 4. Validate player
        if(round.currentPlayer.id != playerId)
            throw InvalidOperationException("Player ($playerId) is not the current player")

        // 5. Get myself
        val me = game.players.find { it.id == round.currentPlayer.id }
        me?: throw NotFoundException("Can't find current player")

        // 6. Validate selected cards
        selectedCards.forEach {
            if (!(me.cards.contains(it)))
                throw InvalidOperationException("You can't select this card: $it")
        }

        // 7. Get new cards
        val deck =  deckService.getDeck(game.id)
        var newCards = selectedCards
        for(x in 0 until 5 - selectedCards.size) newCards = newCards.plus(deck.cards.pop())
        game.players.forEach { if (it.id == playerId) it.cards = newCards }
        deckService.save(deck)

        // 8. Set next player
        round.currentPlayer = if (round.dealer.id == playerId) {
            round.status = RoundStatus.PLAYING
            nextPlayer(game.players, round.goer!!)
        }
        else nextPlayer(game.players, me)

        // 9. Save round
        roundService.save(round)
        save(game)

        // 10. Publish updated game and round
        publishGameAndRound(game, round, me)

        return Pair(getGameForPlayer(game, round, me.id), round)
    }

    fun playCard(gameId: String, playerId: String, myCard: Card): Pair<Game, Round> {
        // 1. Get Game
        var game = get(gameId)

        // 2. Get Round
        var round = roundService.get(game.id!!)
        if (round.status != RoundStatus.PLAYING) throw InvalidOperationException("Can only call in the playing phase")

        // 3. Validate player
        if(round.currentPlayer.id != playerId)
            throw InvalidOperationException("Player ($playerId) is not the current player")

        // 4. Get myself
        val me = game.players.find { it.id == round.currentPlayer.id }
        me?: throw NotFoundException("Can't find current player")

        // 5. Validate selected card
        if (me.cards.isEmpty()) throw InvalidOperationException("You have no cards left")
        if (!(me.cards.contains(myCard)))
            throw InvalidOperationException("You can't select this card: $myCard")

        // 6. Check that they are following suit
        if (round.leadOut == null)
            round.leadOut = myCard
        else if (!isFollowing(round.suit!!, myCard, me.cards, round.currentHand, round.leadOut!!))
            throw InvalidOperationException("Must follow suit!")

        // 7. Play the card
        game.players.forEach {
            if (it.id == me.id) {
                it.cards = it.cards.minus(myCard)
            }
        }
        round.currentHand = round.currentHand.plus(Pair(me.id, myCard))

        // 8. Check if current hand is finished
        if (round.currentHand.size < game.players.size) {
            round.currentPlayer = nextPlayer(game.players, round.currentPlayer)
        } else {
            if (round.completedHands.size >= 4) {
                round.completedHands = round.completedHands.plus(round.currentHand)
                // Calculate Scores
                calculateScores(round, game)

                logger.info("Players after calculating score: ${game.players}")

                // Check if game is over
                game.players.forEach {
                    if (it.score >= 110) {
                        game.status = GameStatus.COMPLETED
                    }
                }
                if (game.status != GameStatus.COMPLETED) {
                    val gameAndRound = deal(game, nextPlayer(game.players, round.dealer))
                    game = gameAndRound.first
                    round = gameAndRound.second
                }
            } else {
                val handWinner = handWinner(round.currentHand, round.suit!!, game.players)
                round.currentPlayer = handWinner.first
                game.players = orderPlayers(handWinner.first, game.players)
                round.completedHands = round.completedHands.plus(round.currentHand)
                round.currentHand = emptyMap()
                round.leadOut = null
            }
        }


        // 9. Save round
        roundService.save(round)
        save(game)

        // 10. Publish updated game and round
        publishGameAndRound(game, round, me)

        return Pair(getGameForPlayer(game, round, me.id), round)
    }

    fun getGameForPlayer(game: Game, round: Round, playerId: String): Game {
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

    private fun publishGameAndRound(game: Game, round: Round, caller: Player) {

        game.players.forEach {
            if (it.id != caller.id && it.id != "dummy") {
                publishService.publishContent(recipient = it.displayName,
                        topic = "/game",
                        content = Pair(getGameForPlayer(game = game, round = round, playerId = it.id), round),
                        gameId = game.id!!,
                        contentType = PublishContentType.GAME_AND_ROUND)
            }
        }
    }

    private fun calculateScores(round: Round, game: Game): List<Player> {
        // 1. Find winner of each hand
        var bestCard: Pair<Player, Card>? = null
        val scores: MutableMap<String, Int> = mutableMapOf()

        round.completedHands.forEach {
            val winner = handWinner(it, round.suit!!, game.players)
            val currentScore = scores[winner.first.teamId]
            if (currentScore == null) scores[winner.first.teamId] = 5
            else scores[winner.first.teamId] = currentScore + 5

            bestCard = if (bestCard == null) winner
            // Both Trumps
            else if (isTrump(round.suit!!, bestCard!!.second) && isTrump(round.suit!!, winner.second)
                        && winner.second.value > bestCard!!.second.value) winner
            // Both Cold
            else if (notTrump(round.suit!!, bestCard!!.second) && notTrump(round.suit!!, winner.second)
                        && winner.second.coldValue > bestCard!!.second.coldValue) winner
            // Only winner is trump
            else if (notTrump(round.suit!!, bestCard!!.second) && isTrump(round.suit!!, winner.second)) winner
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
        val goerScore = scores[round.goer!!.teamId] ?: 0
        if (goerScore >= round.goer!!.call) {
            logger.info("Team ${round.goer!!.teamId} made the contract of ${round.goer!!.call} with a score of $goerScore")
            if (round.goer!!.call == 30) {
                logger.info("Successful Jink called!!")
                updatePlayersScore(game.players, round.goer!!.teamId, goerScore * 2)
            } else {
                updatePlayersScore(game.players, round.goer!!.teamId, goerScore)
            }
            scores.remove(round.goer!!.teamId)
        } else {
            logger.info("Team ${round.goer!!.teamId} did not make the contract of ${round.goer!!.call} with a score of $goerScore")
            updatePlayersScore(game.players, round.goer!!.teamId, -round.goer!!.call)
            scores.remove(round.goer!!.teamId)
        }

        scores.forEach {
            updatePlayersScore(players = game.players, teamId = it.key, points = it.value)
        }

        return game.players
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

    private fun handWinner(currentHand: Map<String, Card>, suit: Suit, players: List<Player>): Pair<Player, Card> {

        // 1. Was a suit or wild card played? If not set the lead out card as the suit
        val activeSuit  = if (currentHand.filter { it.value.suit == suit || it.value.suit == Suit.WILD }.isEmpty())
            currentHand[players[0].id]?.suit
        else suit

        logger.info("Active suit is: $activeSuit")

        // 2. Find winning card
        val winningCard = if (activeSuit == suit) currentHand.filter { it.value.suit == activeSuit || it.value.suit == Suit.WILD }.maxBy { it.value.value }
        else currentHand.filter { it.value.suit == activeSuit }.maxBy { it.value.coldValue }
        winningCard?: throw InvalidOperationException("Can't find the winning card")

        logger.info("Winning card is: $winningCard")

        val winner = players.find { it.id == winningCard.key } ?: throw NotFoundException("Couldn't find winning player")

        logger.info("Winner is: $winner")

        return Pair(winner, winningCard.value)
    }

    private fun isFollowing(suit: Suit, myCard: Card, myCards: List<Card>, playedCards: Map<String, Card>, leadOut: Card): Boolean {
        val suitLead = leadOut.suit == suit || leadOut.suit == Suit.WILD

        if (suitLead) {
            val myTrumps = myCards.filter { it.suit == suit || it.suit == Suit.WILD }
            val winningCard = playedCards.filter { it.value.suit == suit || it.value.suit == Suit.WILD }.maxBy { it.value.value }?.value
                    ?: throw InvalidOperationException("Can't find the winning card")
            return myTrumps.isEmpty() || myCard.suit == suit || myCard.suit == Suit.WILD || canRenage(leadOut, myTrumps)
        }

        val mySuitedCards = myCards.filter { it.suit == leadOut.suit }
        return mySuitedCards.isEmpty() || myCard.suit == suit || myCard.suit == Suit.WILD || myCard.suit == leadOut.suit
    }

    private fun canRenage(leadOut: Card, myTrumps: List<Card>): Boolean {
        myTrumps.forEach {
            if (!it.renegable || it.value <= leadOut.value)
                return false
        }
        return true
    }

    private fun nextPlayer(players: List<Player>, currentPlayer: Player): Player {

        val currentIndex = players.indexOfFirst { it.id == currentPlayer.id }
        if (currentIndex < players.size - 1) {
            if (players[currentIndex + 1].id == "dummy")
                return nextPlayer(players, players[currentIndex + 1])
            return players[currentIndex + 1]
        }
        return players[0]
    }

    private fun previousPlayer(players: List<Player>, currentPlayer: Player): Player {
        val currentIndex = players.indexOf(currentPlayer)
        if (currentIndex == 0) return players.last()
        return players[currentIndex + 1]
    }

    private fun orderPlayersAtStartOfGame(dealer: Player, players: List<Player>): List<Player> {
        val dummy = players.find { it.id == "dummy" }
        val playersSansDummy = if (dummy != null) players.minus(dummy)
        else players
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