package ie.daithi.cards.web.controller

import ie.daithi.cards.enumeration.Card
import ie.daithi.cards.enumeration.Suit
import ie.daithi.cards.model.Game
import ie.daithi.cards.model.Player
import ie.daithi.cards.model.Round
import ie.daithi.cards.repositories.AppUserRepo
import ie.daithi.cards.service.GameService
import ie.daithi.cards.service.RoundService
import ie.daithi.cards.web.exceptions.NotFoundException
import ie.daithi.cards.web.model.CreateGame
import io.swagger.annotations.*
import org.apache.logging.log4j.LogManager
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
@Api(tags = ["Game"], description = "Endpoints that relate to CRUD operations on Games")
class GameController (
        private val gameService: GameService,
        private val roundService: RoundService,
        private val appUserRepo: AppUserRepo
){


    @GetMapping("/admin/game")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Get Game", notes = "Get the game")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful"),
            ApiResponse(code = 404, message = "Game not found")
    )
    @ResponseBody
    fun get(@RequestParam id: String): Game {
        return gameService.get(id)
    }

    @GetMapping("/admin/game/all")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Get all games", notes = "Get all games")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful")
    )
    @ResponseBody
    fun getAll(): List<Game> {
        return gameService.getAll()
    }

    @GetMapping("/admin/game/active")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Get all active games", notes = "Get all active games")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful")
    )
    @ResponseBody
    fun getActive(): List<Game> {
        return gameService.getActive()
    }

    @PutMapping("/admin/game")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Create Game", notes = "Issues an email to all players with a link to allow them to access the game")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful"),
            ApiResponse(code = 502, message = "An error occurred when attempting to send email")
    )
    @ResponseBody
    fun put(@RequestBody createGame: CreateGame): Game {
        return gameService.create(createGame.name, createGame.playerEmails, createGame.emailMessage)
    }

    @PutMapping("/admin/game/finish")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Finish a Game", notes = "Finishes the game")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful"),
            ApiResponse(code = 404, message = "Game not found")
    )
    @ResponseBody
    fun finish(@RequestParam id: String) {
        return gameService.finish(id)
    }

    @PutMapping("/admin/game/cancel")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Cancel a Game", notes = "Cancels the game")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful"),
            ApiResponse(code = 404, message = "Game not found")
    )
    @ResponseBody
    fun cancel(@RequestParam id: String) {
        return gameService.cancel(id)
    }

    @DeleteMapping("/admin/game")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Delete Game", notes = "Delete the game")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful"),
            ApiResponse(code = 404, message = "Game not found")
    )
    fun delete(@RequestParam id: String) {
        return gameService.delete(id)
    }


    @PutMapping("/admin/deal")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Deal round", notes = "Shuffles the deck and create a new round")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful"),
            ApiResponse(code = 502, message = "An error occurred when attempting to send email")
    )
    @ResponseBody
    fun deal(@RequestParam gameId: String): Pair<Game, Round> {
        val game = gameService.get(gameId)
        val gameAndRound = gameService.deal(game, null)
        gameService.save(gameAndRound.first)
        roundService.save(gameAndRound.second)
        return gameAndRound
    }

    @PutMapping("/call")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Make a call", notes = "After dealing each player can make a call between 0-30")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful")
    )
    @ResponseBody
    fun call(@RequestParam call: Int): Pair<Game, Round> {
        val id = SecurityContextHolder.getContext().authentication.name
        val appUser = appUserRepo.findByUsernameIgnoreCase(id) ?: throw NotFoundException("User not found")
        val game = gameService.getByPlayerId(appUser.id!!)
        return gameService.call(gameId = game.id!!, playerId = appUser.id!!, call = call)
    }

    @PutMapping("/buyCards")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Buy Cards", notes = "Buy cards")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful")
    )
    @ResponseBody
    fun buyCards(@RequestBody cards: List<Card>): Pair<Game, Round> {
        val id = SecurityContextHolder.getContext().authentication.name
        val appUser = appUserRepo.findByUsernameIgnoreCase(id) ?: throw NotFoundException("User not found")
        val game = gameService.getByPlayerId(appUser.id!!)
        return gameService.buyCards(gameId = game.id!!, playerId = appUser.id!!, selectedCards = cards)
    }

    @PutMapping("/chooseFromDummy")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Choose from the dummy", notes = "Choose cards from the dummy")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful")
    )
    @ResponseBody
    fun chooseFromDummy(@RequestBody cards: List<Card>, @RequestParam suit: Suit): Pair<Game, Round> {
        val id = SecurityContextHolder.getContext().authentication.name
        val appUser = appUserRepo.findByUsernameIgnoreCase(id) ?: throw NotFoundException("User not found")
        val game = gameService.getByPlayerId(appUser.id!!)
        return gameService.chooseFromDummy(gameId = game.id!!, playerId = appUser.id!!, selectedCards = cards, suit = suit)
    }

    @PutMapping("/playCard")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Play a card", notes = "Play a card")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful")
    )
    @ResponseBody
    fun playCard(@RequestParam card: Card): Pair<Game, Round> {
        val id = SecurityContextHolder.getContext().authentication.name
        val appUser = appUserRepo.findByUsernameIgnoreCase(id) ?: throw NotFoundException("User not found")
        val game = gameService.getByPlayerId(appUser.id!!)
        return gameService.playCard(gameId = game.id!!, playerId = appUser.id!!, myCard = card)
    }

    @GetMapping("/game")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Get game", notes = "Get the game")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful")
    )
    @ResponseBody
    fun getGame(): Game {
        val id = SecurityContextHolder.getContext().authentication.name
        val appUser = appUserRepo.findByUsernameIgnoreCase(id) ?: throw NotFoundException("User not found")
        val game = gameService.getByPlayerId(appUser.id!!)
        val round = roundService.get(gameId = game.id!!)
        return gameService.getGameForPlayer(game = game, round = round, playerId = appUser.id!!)
    }

    @GetMapping("/round")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Get the current round", notes = "Get the current round")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful")
    )
    @ResponseBody
    fun getRound(): Round {
        val id = SecurityContextHolder.getContext().authentication.name
        val appUser = appUserRepo.findByUsernameIgnoreCase(id) ?: throw NotFoundException("User not found")
        val game = gameService.getByPlayerId(appUser.id!!)
        return roundService.get(gameId = game.id!!)
    }

    @GetMapping("/gameAndRound")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Get the current game and round", notes = "Get the current game and round")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful")
    )
    @ResponseBody
    fun getGameAndRound(): Pair<Game, Round> {
        val id = SecurityContextHolder.getContext().authentication.name
        val appUser = appUserRepo.findByUsernameIgnoreCase(id) ?: throw NotFoundException("User not found")
        val gameFull = gameService.getByPlayerId(appUser.id!!)
        val round = roundService.get(gameId = gameFull.id!!)
        val game = gameService.getGameForPlayer(game = gameFull, round = round, playerId = appUser.id!!)
        return Pair(game, round)
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java)
    }
}