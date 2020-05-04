package ie.daithi.cards.service

import ie.daithi.cards.enumeration.Card
import ie.daithi.cards.enumeration.GameStatus
import ie.daithi.cards.enumeration.RoundStatus
import ie.daithi.cards.enumeration.Suit
import ie.daithi.cards.model.Game
import ie.daithi.cards.model.Hand
import ie.daithi.cards.model.Player
import ie.daithi.cards.model.Round
import ie.daithi.cards.repositories.mongodb.AppUserRepo
import ie.daithi.cards.repositories.mongodb.GameRepo
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

        game = gameRepo.save(game)

        logger.info("Game started successfully ${game.id}")
        return game
    }

    fun get(id: String): Game {
        val game = gameRepo.findById(id)
        if (!game.isPresent)
            throw NotFoundException("Game $id not found")
        return game.get()
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
        gameRepo.save(game)
    }

    fun cancel(id: String) {
        val game = get(id)
        if( game.status == GameStatus.CANCELLED) throw InvalidSatusException("Game is already in CANCELLED state")
        game.status = GameStatus.CANCELLED
        gameRepo.save(game)
    }

    // Deal a new round
    fun deal(gameId: String): Round {
        // 1. Get Game
        val game = get(gameId)

        // 2. Get Dealer
        val lastRound = roundService.getOrNull(gameId)
        val dealer = if (lastRound != null) lastRound.hands[0].player
        else game.players[0]

        // 3. Shuffle
        deckService.shuffle(gameId = game.id!!)

        // 4. Order the hands. Dummy second last, dealer last
        val hands = orderHands(dealer, game.players)

        // 5. Deal the cards
        val deck =  deckService.getDeck(game.id)
        for(x in 0 until 5) {
            hands.forEach {
                it.cards = it.cards.plus(deck.cards.pop())
            }
        }
        deckService.save(deck)

        // 6. Store the round
        val round = Round(id = game.id,
                dealer = dealer,
                status = RoundStatus.CALLING,
                currentPlayer = hands[0].player,
                hands = hands)

        roundService.save(round)

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

        // 4. Get my hand
        val myHand = round.hands.find { it.player.id == round.currentPlayer.id }
        myHand?: throw NotFoundException("Can't find current player's hand")

        // 5. Check they are the next player
        if (round.currentPlayer.id != playerId) throw InvalidOperationException("It's not your go!")

        // 6. Check if call value is valid i.e. > all other calls
        if (call != 0) {
            val currentCall = round.hands.maxBy { it.call }
            currentCall?:throw Exception("This should never happen")
            if (round.currentPlayer == round.dealer && call >= currentCall.call) myHand.call = call
            else if (call > currentCall.call) myHand.call = call
            else throw InvalidOperationException("Call must be higher than ${currentCall.call}")
        }

        // 7. Set next player/round status
        if (call == 30) {
            round.status = RoundStatus.CALLED
            round.goer = myHand.player
            round.currentPlayer = myHand.player
        } else if (round.currentPlayer.id == round.dealer.id) {
            val callingHand = round.hands.maxBy { it.call }
            callingHand ?: throw Exception("This should never happen")
            if (callingHand.call == 0) return deal(gameId)
            round.status = RoundStatus.CALLED
            if (myHand.call == callingHand.call) {
                round.goer = myHand.player
                round.currentPlayer = myHand.player
            } else {
                round.goer = callingHand.player
                round.currentPlayer = callingHand.player
            }

        } else {
            val myHandIndex = round.hands.indexOf(myHand)
            var nextHand = round.hands[myHandIndex + 1]
            if (nextHand.player.id == "dummy") nextHand = round.hands[myHandIndex + 2]
            round.currentPlayer = nextHand.player
        }

        // 8. Save round
        roundService.save(round)

        // 9. Filter out other players cards for return
        round.hands.forEach { if (it.player.id != myHand.player.id) it.cards = emptyList() }

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
        val myHand = round.hands.find { it.player.id == round.currentPlayer.id }
        myHand?: throw NotFoundException("Can't find current player's hand")

        // 6. Get Dummy
        val dummy = round.hands.find { it.player.id == "dummy" }
        dummy?: throw NotFoundException("Can't find dummy")

        // 5. Validate selected cards
        selectedCards.forEach {
            if (!(myHand.cards.contains(it) || dummy.cards.contains(it)))
                throw InvalidOperationException("You can't select this card: $it")
        }

        // 6. Set new hand and remove the dummy
        round.hands = round.hands.minus(dummy)
        round.hands.forEach { if (it.player.id == playerId) it.cards = selectedCards }
        round.status = RoundStatus.BUYING
        round.currentPlayer = nextPlayer(game.players, round.dealer)
        round.suit = trumps

        // 7. Save round
        roundService.save(round)

        // 8. Filter out other players cards for return
        round.hands.forEach { if (it.player.id != myHand.player.id) it.cards = emptyList() }

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

        // 5. Get my hand
        val myHand = round.hands.find { it.player.id == round.currentPlayer.id }
        myHand?: throw NotFoundException("Can't find current player's hand")

        // 6. Validate selected cards
        selectedCards.forEach {
            if (!(myHand.cards.contains(it)))
                throw InvalidOperationException("You can't select this card: $it")
        }

        // 7. Get new cards
        val deck =  deckService.getDeck(game.id)
        var newCards = selectedCards
        for(x in 0 until 5 - selectedCards.size) newCards = newCards.plus(deck.cards.pop())
        round.hands.forEach { if (it.player.id == playerId) it.cards = newCards }
        deckService.save(deck)

        // 8. Set new hand
        if (round.dealer.id == playerId) round.status = RoundStatus.PLAYING
        round.currentPlayer = if (round.dealer.id == playerId) nextPlayer(game.players, round.goer!!)
        else nextPlayer(game.players, myHand.player)

        // 9. Save round
        roundService.save(round)

        // 10. Filter out other players cards for return
        round.hands.forEach { if (it.player.id != myHand.player.id) it.cards = emptyList() }

        return round
    }

    fun playCard(gameId: String, playerId: String, card: Card): Round {
        // 1. Get Game
        val game = get(gameId)

        // 2. Get Round
        val round = roundService.get(game.id!!)
        if (round.status != RoundStatus.PLAYING) throw InvalidOperationException("Can only call in the playing phase")

        // 3. Validate player
        if(round.currentPlayer.id != playerId)
            throw InvalidOperationException("Player ($playerId) is not the current player")

        // 4. Get my hand
        val myHand = round.hands.find { it.player.id == round.currentPlayer.id }
        myHand?: throw NotFoundException("Can't find current player's hand")

        // 5. Validate selected card
        if (myHand.cards.isEmpty()) throw InvalidOperationException("You have no cards left")
        if (!(myHand.cards.contains(card)))
            throw InvalidOperationException("You can't select this card: $card")

        // TODO Need to check they are following suit

        // 6. Play the card
        round.hands.forEach {
            if (it.player.id == playerId) {
                it.played.plus(card)
                it.cards.minus(card)
            }
        }

        // 7. Check if point is finished
        val yetToPlay = round.hands.filter {
            it.played.size < round.cardNumber
        }
        if (yetToPlay.isNotEmpty()) {
            round.currentPlayer = nextPlayer(game.players, round.currentPlayer)
        } else {
            if (round.cardNumber == 5) {
                // TODO Need to persist scores
//                calculateScores(round)
                deal(gameId)
            } else {
                // TODO Need to check who won the round and then set them as the current player
                round.cardNumber++
            }
        }


        // 8. Save round
        roundService.save(round)

        // 9. Filter out other players cards for return
        round.hands.forEach { if (it.player.id != myHand.player.id) it.cards = emptyList() }
        return round
    }

    fun nextPlayer(players: List<Player>, previousPlayer: Player): Player {
        return generateSequence { players }.flatten().elementAt(players.indexOf(previousPlayer) + 1)
    }

    fun orderHands(dealer: Player, players: List<Player>): List<Hand> {
        var handStack = Stack<Hand>()
        handStack.push(Hand(player = dealer))
        handStack.push(Hand(player = Player("dummy","dummy")))
        var currentIndex = players.indexOf(dealer)
        for(x in 0 until players.size - 1) {
            currentIndex = if(currentIndex < 1)
                players.size - 1
            else currentIndex - 1
            handStack.push(Hand(player = players[currentIndex]))
        }
        return handStack.toMutableList().reversed()
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java)
        private val secureRandom = SecureRandom()
        private val VALID_CALL = listOf(0, 10, 15, 20, 25, 30)
    }

}