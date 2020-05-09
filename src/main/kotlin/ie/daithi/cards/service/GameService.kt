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
        private val roundService: RoundService
) {
    fun create(name: String, playerEmails: List<String>): Game {
        logger.info("Attempting to start a 110")

        // 1. Put all emails to lower case
        val lowerCaseEmails = playerEmails.map(String::toLowerCase)

        // 2. Validate Emails
        lowerCaseEmails.forEach {
            if (!emailValidator.isValid(it))
                throw InvalidEmailException("Invalid email $it")
        }

        // 3. Create Players and Issue emails
        val users = arrayListOf<AppUser>()

        lowerCaseEmails.forEach{
            val passwordByte = ByteArray(16)
            secureRandom.nextBytes(passwordByte)
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(passwordByte)
            val password = digest.fold("", { str, byt -> str + "%02x".format(byt) })

            val user = AppUser(username = it,
                    password = passwordEncoder.encode(password),
                    authorities = listOf(Authority.PLAYER))

            appUserRepo.deleteByUsernameIgnoreCase(it)
            appUserRepo.save(user)
            users.add(user)
            emailService.sendQuizInvite(it, password)
        }

        // 5. Create Game
        var game = Game(
                status = GameStatus.ACTIVE,
                name = name,
                players = users.map { Player(id = it.id!!, displayName = it.username!!) })

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
    fun deal(gameId: String): Round {
        // 1. Get Game
        val game = get(gameId)

        // 2. Get Dealer
        val dealer = game.players[0]

        // 3. Shuffle
        deckService.shuffle(gameId = game.id!!)

        // 4. Order the hands. Dummy second last, dealer last
        game.players = orderPlayersAtStartOfGame(dealer, game.players)

        // 5. Deal the cards
        val deck =  deckService.getDeck(game.id)
        for(x in 0 until 5) {
            game.players.forEach {
                it.cards = it.cards.plus(deck.cards.pop())
            }
        }
        deckService.save(deck)

        // 6. Store the round
        val round = Round(id = game.id,
                dealer = dealer,
                status = RoundStatus.CALLING,
                currentPlayer = game.players[0])

        roundService.save(round)
        save(game)

        // 7. Return the round
        return round
    }

    fun call(gameId: String, playerId: String, call: Int): Round {
        // 1. Validate call value
        if(!VALID_CALL.contains(call)) throw InvalidOperationException("Call value $call is invalid")

        // 2. Get Game
        val game = get(gameId)

        // 3. Get Round
        val round = roundService.get(game.id!!)
        if (round.status != RoundStatus.CALLING) throw InvalidOperationException("Can only call in the calling phase")

        // 4. Find myself
        val me = game.players.find { it.id == round.currentPlayer.id }
        me?: throw NotFoundException("Can't find current player")

        // 5. Check they are the next player
        if (round.currentPlayer.id != playerId) throw InvalidOperationException("It's not your go!")

        // 6. Check if call value is valid i.e. > all other calls
        if (call != 0) {
            val currentCaller = game.players.maxBy { it.call }
            currentCaller?:throw Exception("This should never happen")
            if (round.currentPlayer == round.dealer && call >= currentCaller.call) me.call = call
            else if (call > currentCaller.call) me.call = call
            else throw InvalidOperationException("Call must be higher than ${currentCaller.call}")
        }

        // 7. Set next player/round status
        if (call == 30) {
            round.status = RoundStatus.CALLED
            round.goer = me
            round.currentPlayer = me
        } else if (round.currentPlayer.id == round.dealer.id) {
            val caller = game.players.maxBy { it.call }
            caller ?: throw Exception("This should never happen")
            if (caller.call == 0) return deal(gameId)
            round.status = RoundStatus.CALLED
            if (me.call == caller.call) {
                round.goer = me
                round.currentPlayer = me
            } else {
                round.goer = caller
                round.currentPlayer = caller
            }

        } else {
            val myIndex = game.players.indexOf(me)
            var nextPlayer = game.players[myIndex + 1]
            if (nextPlayer.id == "dummy") nextPlayer = game.players[myIndex + 2]
            round.currentPlayer = nextPlayer
        }

        // 8. Save round
        roundService.save(round)

        return round
    }

    fun chooseFromDummy(gameId: String, playerId: String, selectedCards: List<Card>, trumps: Suit): Round {
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
        round.suit = trumps

        // 7. Save round
        roundService.save(round)
        save(game)

        return round
    }

    fun buyCards(gameId: String, playerId: String, selectedCards: List<Card>): Round {
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
        if (round.dealer.id == playerId) round.status = RoundStatus.PLAYING
        round.currentPlayer = if (round.dealer.id == playerId) nextPlayer(game.players, round.goer!!)
        else nextPlayer(game.players, me)

        // 9. Save round
        roundService.save(round)
        save(game)

        return round
    }

    fun playCard(gameId: String, playerId: String, myCard: Card): Round {
        // 1. Get Game
        val game = get(gameId)

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

        // 6. Check that they are collowing suit
        if (round.leadOut == null)
            round.leadOut = myCard.suit
        else if (!isFollowing(round.suit!!, myCard, me.cards, round.currentHand, round.leadOut!!))
            throw InvalidOperationException("Must follow suit!")

        // 7. Play the card
        executePlayCard(game.players, round, me, myCard)

        // 8. Check if current hand is finished
        if (round.currentHand.size < game.players.size) {
            round.currentPlayer = nextPlayer(game.players, round.currentPlayer)
        } else {
            if (round.completedHands.size >= 4) {
                // Calculate Scores
                game.players = calculateScores(round.completedHands, round.suit!!, game.players)

                // TODO Do better. Also need teams.
                // Check if game is over
                game.players.forEach {
                    if (it.score > 110) throw Exception("Game over! ${it.displayName} has won")
                }
                round = deal(gameId)
            } else {
                val handWinner = handWinner(round.currentHand, round.suit!!, game.players)
                round.currentPlayer = handWinner.first
                game.players = orderPlayers(handWinner.first, game.players)
                round.completedHands = round.completedHands.plus(round.currentHand)
                round.currentHand = emptyMap()
            }
        }


        // 9. Save round
        roundService.save(round)
        save(game)

        return round
    }

    fun getGameForPlayer(gameId: String, playerId: String): Game {
        // 1. Get Game
        val game = get(gameId)

        // 2. Filter out other player's cards
        game.players.forEach { if (it.id != playerId) it.cards = emptyList() }
        return game
    }

    private fun calculateScores(playedHands: List<Map<String, Card>>, suit: Suit, players: List<Player>): List<Player> {
        // 1. Find winner of each hand
        var bestCard: Pair<Player, Card>? = null
        var updatedPlayers = players

        playedHands.forEach {
            val winner = handWinner(it, suit, players)
            updatedPlayers = updatePlayersScore(updatedPlayers, winner.first, 5)
            bestCard = if (bestCard == null || winner.second.value > bestCard!!.second.value) winner
            else bestCard
        }

        // 2. Add points for best card
        bestCard ?: throw InvalidOperationException("No best card identified")
        updatedPlayers = updatePlayersScore(updatedPlayers, bestCard!!.first, 5)

        // 3. Add points for best card

        return updatedPlayers
    }

    private fun updatePlayersScore(players: List<Player>, player: Player, points: Int): List<Player> {
        var updatedPlayers = players
        updatedPlayers.forEach {
            if(it.id == player.id) it.score = it.score + points
        }
        return players
    }

    private fun handWinner(currentHand: Map<String, Card>, suit: Suit, players: List<Player>): Pair<Player, Card> {

        // 1. Was a suit or wild card played? If not set the lead out card as the suit
        val activeSuit  = if (currentHand.filter { it.value.suit == suit || it.value.suit == Suit.WILD }.isEmpty())
            currentHand[players[0].id]?.suit
        else suit

        // 2. Find winning card
        val winningCard = currentHand.filter { it.value.suit == activeSuit || it.value.suit == Suit.WILD }.maxBy { it.value.value }
                ?: throw InvalidOperationException("Can't find the winning card")

        val winner = players.find { it.id == winningCard.key } ?: throw NotFoundException("Couldn't find winning player")

        return Pair(winner, winningCard.value)
    }

    private fun isFollowing(suit: Suit, myCard: Card, myCards: List<Card>, playedCards: Map<String, Card>, leadOut: Suit): Boolean {
        val suitLead = leadOut == suit || leadOut == Suit.WILD

        if (suitLead) {
            val myTrumps = myCards.filter { it.suit == suit || it.suit == Suit.WILD }
            val winningCard = playedCards.filter { it.value.suit == suit || it.value.suit == Suit.WILD }.maxBy { it.value.value }?.value
                    ?: throw InvalidOperationException("Can't find the winning card")
            return myTrumps.isEmpty() || myCard.suit == suit || myCard.suit == Suit.WILD || canRenage(winningCard, myTrumps)
        }

        val mySuitedCards = myCards.filter { it.suit == leadOut }
        return mySuitedCards.isEmpty() || myCard.suit == suit || myCard.suit == Suit.WILD || myCard.suit == leadOut
    }

    private fun canRenage(winningCard: Card, myTrumps: List<Card>): Boolean {
        myTrumps.forEach {
            if (!it.renegable || it.value <= winningCard.value)
                return false
        }
        return true
    }

    private fun executePlayCard(players: List<Player>, round: Round, me: Player, myCard: Card) {
        players.forEach {
            if (it.id == me.id) {
                it.cards.minus(myCard)
            }
        }
        round.currentHand = round.currentHand.plus(Pair(me.id, myCard))
    }

    private fun nextPlayer(players: List<Player>, currentPlayer: Player): Player {
        return generateSequence { players }.flatten().elementAt(players.indexOf(currentPlayer) + 1)
    }

    private fun previousPlayer(players: List<Player>, currentPlayer: Player): Player {
        return generateSequence { players }.flatten().elementAt(players.indexOf(currentPlayer) - 1)
    }

    private fun orderPlayersAtStartOfGame(dealer: Player, players: List<Player>): List<Player> {
        var playerStack = Stack<Player>()
        playerStack.push(dealer)
        playerStack.push(Player("dummy","dummy"))
        var currentIndex = players.indexOf(dealer)
        for(x in 0 until players.size - 1) {
            currentIndex = if(currentIndex < 1)
                players.size - 1
            else currentIndex - 1
            playerStack.push(players[currentIndex])
        }
        return playerStack.toMutableList().reversed()
    }

    private fun orderPlayers(currentPlayer: Player, players: List<Player>): List<Player> {
        var reorderedPlayers = emptyList<Player>()
        var currentIndex = players.indexOf(currentPlayer)
        val sequence = generateSequence { players }.flatten()

        for(x in 0 until players.size - 1) {
            reorderedPlayers = reorderedPlayers.plus(sequence.elementAt(x + currentIndex))
        }
        return reorderedPlayers
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java)
        private val secureRandom = SecureRandom()
        private val VALID_CALL = listOf(0, 10, 15, 20, 25, 30)
    }

}