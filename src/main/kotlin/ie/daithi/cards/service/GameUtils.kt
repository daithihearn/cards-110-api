package ie.daithi.cards.service

import ie.daithi.cards.enumeration.Card
import ie.daithi.cards.enumeration.RoundStatus
import ie.daithi.cards.enumeration.Suit
import ie.daithi.cards.model.*
import ie.daithi.cards.web.exceptions.GameNotOverException
import ie.daithi.cards.web.exceptions.InvalidOperationException
import ie.daithi.cards.web.exceptions.NotFoundException
import ie.daithi.cards.web.model.enums.EventType
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import java.rmi.UnexpectedException
import java.time.LocalDateTime
import java.util.*
import kotlin.jvm.Throws

@Service
class GameUtils(
    private val publishService: PublishService,
    private val spectatorService: SpectatorService
    ) {

    fun popFromDeck(deck: Deck): Card {
        if (deck.cards.empty()) throw NotFoundException("The deck(${deck.id}) is empty. This should never happen.")
        return deck.cards.pop()
    }

    fun isGameOver(players: List<Player>): Boolean {
        return players.maxOf { player -> player.score } >= 110
    }

    @Throws(GameNotOverException::class)
    fun applyWinners(game: Game) {
        val winners = findWinners(game)

        game.players.forEach { player ->
            if (winners.find { winner -> winner.id == player.id } != null) player.winner = true
        }
    }

    @Throws(GameNotOverException::class)
    fun findWinners(game: Game): List<Player> {
        // 1. Check that the game is over
        if (!isGameOver(game.players))
            throw GameNotOverException("Game isn't over")

        // 2. If only one team >= 110 -> they are the winner
        val playersOver110 = game.players.filter { player -> player.score >= 110 }
        if (playersOver110.size == 1 || (game.players.size == 6 && playersOver110.size == 2))
            return playersOver110

        // 3. If more than one team >= 110 but one is the goer -> the goer is the winning team
        val winningGoer = playersOver110.find { player -> player.id == game.currentRound.goerId }
        if (winningGoer != null)
            return playersOver110.filter { player -> player.teamId == winningGoer.teamId }

        // 4. If none of the above -> first past the post wins
        return calculateFirstPastThePost(game.currentRound, game.players)
    }

    fun parsePlayerGameState(player: Player, game: Game): GameState {

        // 1. Find dummy
        val dummy = (game.currentRound.goerId == player.id).let {
            game.players.find { player -> player.id == "dummy" }
        }

        // 2. Get max call
        val highestCaller = game.players.maxByOrNull { p -> p.call }

        // 3. Add dummy if applicable
        val iamGoer = game.currentRound.goerId == player.id
        if (iamGoer && RoundStatus.CALLED == game.currentRound.status && dummy != null)
            player.cards = player.cards.plus(dummy.cards)

        // 4. Return player's game state
        return GameState(
                id = game.id!!,
                me = player,
                iamSpectator = false,
                isMyGo = game.currentRound.currentHand.currentPlayerId == player.id,
                iamGoer = iamGoer,
                iamDealer = game.currentRound.dealerId == player.id,
                cards = player.cards,
                status = game.status,
                round = game.currentRound,
                maxCall = highestCaller?.call ?: 0,
                playerProfiles = game.players.filter { p -> p.id != "dummy" }
        )
    }

    fun parseSpectatorGameState(game: Game): GameState {
        // 1. Return spectator's game state
        return GameState(
            id = game.id!!,
            iamSpectator = true,
            isMyGo = false,
            iamGoer = false,
            iamDealer = false,
            status = game.status,
            round = game.currentRound,
            playerProfiles = game.players.filter { p -> p.id != "dummy" }
        )
    }

    fun completeHand(game: Game): Game {

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

    fun completeRound(game: Game): Game {
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

    fun applyScores(game: Game): List<Player> {
        // 1. Calculate the scores for the round
        val scores = calculateScoresForRound(game.currentRound, game.players)

        // 2. Apply the calculated scores
        applyScoresForRound(game, scores)

        return game.players
    }

    fun calculateScoresForRound(round: Round, players: List<Player>): MutableMap<String, Int> {

        val scores: MutableMap<String, Int> = mutableMapOf()

        // 1. Find the best card played
        val bestCard = findBestCardPlayed(round)

        // 2. Calculate winners
        round.completedHands.plus(round.currentHand).forEach { hand ->
            val winner = handWinner(hand, round.suit!!, players)
            val currentScore = scores[winner.first.teamId]
            if (currentScore == null) scores[winner.first.teamId] = if (winner.second == bestCard) 10 else 5
            else scores[winner.first.teamId] = if (winner.second == bestCard) currentScore + 10 else currentScore + 5
        }

        return scores
    }

    fun applyScoresForRound(game: Game, scores: MutableMap<String, Int>): Game {
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

    fun calculateFirstPastThePost(round: Round, players: List<Player>): List<Player> {
        // 1. Find the best card played
        val bestCard = findBestCardPlayed(round)

        // 2. Get current team scores
        val scores: MutableMap<String, Int> = mutableMapOf()
        players.forEach { player -> scores[player.teamId] = player.score }

        // 3. Go backwards through the rounds until there is a unique winner
        if (checkIfUniqueWinner(scores)) return players.filter { player -> player.score >= 110 }
        round.completedHands.plus(round.currentHand).reversed().forEach { hand ->
            val winner = handWinner(hand, round.suit!!, players)
            val currentScore = scores[winner.first.teamId] ?: throw UnexpectedException("Can't resolve the winner. This can only happen if the game is corrupt!")
            scores[winner.first.teamId] = if (winner.second == bestCard) currentScore - 10 else currentScore - 5
            if (checkIfUniqueWinner(scores)) return players.filter { player -> player.score >= 110 }
        }
        // This should never happen
        throw UnexpectedException("Can't resolve the winner. This can only happen if the game is corrupt!")
    }

    fun checkIfUniqueWinner(currentScores: MutableMap<String, Int>): Boolean {
        val scoresOver110 = currentScores.filter { score -> score.value >= 110 }
        if (scoresOver110.isEmpty()) throw Exception("Only call this function if there are scores over 110")
        return scoresOver110.size == 1
    }

    fun findBestCardPlayed(round: Round): Card {

        val suit = round.suit ?: throw InvalidOperationException("No suit detected")
        var bestCard = Card.EMPTY
        round.completedHands.plus(round.currentHand).forEach { hand ->
            hand.playedCards.forEach { card ->
                if (isTrump(suit, card.value) && card.value.value > bestCard.value) bestCard = card.value
            }
        }
        return bestCard
    }

    fun findPlayer(players: List<Player>, playerId: String): Player {
        return players.find { it.id == playerId } ?: throw NotFoundException("Can't find the player")
    }

    fun isTrump(suit: Suit, card: Card): Boolean {
        return card.suit == suit || card.suit == Suit.WILD
    }

    fun notTrump(suit: Suit, card: Card): Boolean {
        return !isTrump(suit, card)
    }

    fun updatePlayersScore(players: List<Player>, teamId: String, points: Int) {

        players.forEach { player ->
            if(player.teamId == teamId) {
                // Update Score
                player.score = player.score + points
                // Update Rings
                if (points < 0) player.rings = player.rings + 1
            }
        }
    }

    fun handWinner(currentHand: Hand, suit: Suit, players: List<Player>): Pair<Player, Card> {

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

    fun isFollowing(myCard: Card, myCards: List<Card>, currentHand: Hand, suit: Suit): Boolean {
        val suitLead = currentHand.leadOut!!.suit == suit || currentHand.leadOut!!.suit == Suit.WILD

        if (suitLead) {
            val myTrumps = myCards.filter { it.suit == suit || it.suit == Suit.WILD }
            return myTrumps.isEmpty() || myCard.suit == suit || myCard.suit == Suit.WILD || canRenage(currentHand.leadOut!!, myTrumps)
        }

        val mySuitedCards = myCards.filter { it.suit == currentHand.leadOut!!.suit }
        return mySuitedCards.isEmpty() || myCard.suit == suit || myCard.suit == Suit.WILD || myCard.suit == currentHand.leadOut!!.suit
    }

    fun canRenage(leadOut: Card, myTrumps: List<Card>): Boolean {
        myTrumps.forEach {
            if (!it.renegable || it.value <= leadOut.value)
                return false
        }
        return true
    }

    fun nextPlayer(players: List<Player>, currentPlayerId: String): Player {

        val currentIndex = players.indexOfFirst { it.id == currentPlayerId }
        if (currentIndex == -1) throw NotFoundException("Can't find player $currentPlayerId")
        if (currentIndex < players.size - 1) {
            if (players[currentIndex + 1].id == "dummy")
                return nextPlayer(players, players[currentIndex + 1].id)
            return players[currentIndex + 1]
        }
        return players[0]
    }

    fun orderPlayersAtStartOfGame(dealerId: String, players: List<Player>): List<Player> {
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

    fun orderPlayers(currentPlayer: Player, players: List<Player>): List<Player> {
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

    fun validateNumberOfCardsSelectedWhenBuying(cardsSelected: Int, numPlayers: Int) {
        val numberHaveToKeep = when (numPlayers) {
            in 2..4 -> 0
            5 -> 1
            else -> 2
        }

        if (cardsSelected < numberHaveToKeep) throw InvalidOperationException("You must choose at least $numberHaveToKeep cards")
        else if (cardsSelected > 5) throw InvalidOperationException("You can only choose 5 cards")
    }

    fun publishBuyCardsEvent(game: Game, buyCardsEvent: BuyCardsEvent) {

        // Publish for players
        game.players.forEach { player ->
            if (player.id != "dummy" && player.id != buyCardsEvent.playerId) {
                publishService.publishContent(recipient = "${player.id}${game.id!!}",
                    content = buyCardsEvent,
                    contentType = EventType.BUY_CARDS_NOTIFICATION)
            }
        }

        // Publish for spectators
        spectatorService.getSpectators(game.id!!).forEach { spectator ->
            publishService.publishContent(recipient = "${spectator.id}${game.id}",
                content = buyCardsEvent,
                contentType = EventType.BUY_CARDS_NOTIFICATION)
        }
    }

    fun publishGame(game: Game, type: EventType) {

        // Publish for players
        game.players.forEach { player ->
            if (player.id != "dummy") {
                publishService.publishContent(recipient = "${player.id}${game.id!!}",
                    content = parsePlayerGameState(player = player, game = game),
                    contentType = type)
            }
        }

        // Publish for spectators
        spectatorService.getSpectators(game.id!!).forEach { spectator ->
            publishService.publishContent(recipient = "${spectator.id}${game.id}",
                content = parseSpectatorGameState(game = game),
                contentType = type)
        }
    }

    companion object {
        private val logger = LogManager.getLogger(GameUtils::class.java)
    }

}
