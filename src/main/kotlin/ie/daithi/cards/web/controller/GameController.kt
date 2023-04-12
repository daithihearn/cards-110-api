package ie.daithi.cards.web.controller

import ie.daithi.cards.enumeration.Card
import ie.daithi.cards.enumeration.Suit
import ie.daithi.cards.model.Game
import ie.daithi.cards.service.AppUserService
import ie.daithi.cards.service.GameService
import ie.daithi.cards.web.model.CreateGame
import ie.daithi.cards.model.AppUser
import ie.daithi.cards.model.GameState
import ie.daithi.cards.utils.GameUtils
import ie.daithi.cards.service.SpectatorService
import ie.daithi.cards.web.exceptions.ForbiddenException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.apache.logging.log4j.LogManager
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Game", description = "Endpoints that relate to CRUD operations on Games")
class GameController (
    private val gameService: GameService,
    private val gameUtils: GameUtils,
    private val appUserService: AppUserService,
    private val spectatorService: SpectatorService
){


    @GetMapping("/game")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Get Game", description = "Get the game for the given gameId")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "Request successful"),
            ApiResponse(responseCode = "404", description = "Game not found")
    )
    @ResponseBody
    fun get(@RequestParam gameId: String): Game {
        return gameService.get(gameId)
    }

    @GetMapping("/game/all")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Get all games", description = "Get all games")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "Request successful")
    )
    @ResponseBody
    fun getAll(): List<Game> {
        return gameService.getAll()
    }

    @GetMapping("/admin/game/players/all")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Get ALL Players", description = "Get all players")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "Request successful")
    )
    @ResponseBody
    fun getAllPlayers(): List<AppUser> {

        return appUserService.getAllUsers()
    }

    @GetMapping("/game/players")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Get Players", description = "Get the players for this game")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "Request successful"),
            ApiResponse(responseCode = "404", description = "Game not found")
    )
    @ResponseBody
    fun getPlayersForGame(@RequestParam gameId: String): List<AppUser> {
        // 1. Get current user ID
//        val subject = SecurityContextHolder.getContext().authentication.name ?: throw ForbiddenException("Couldn't authenticate user")
//        val user = appUserService.getUserBySubject(subject)

        // 2. Get Game
        val game = gameService.get(gameId)

        // 3. Check the player is in this game
//        if (!game.players.map {player -> player.id }.contains(user.id!!) && game.adminId != user.id!!) throw ForbiddenException("Can only get players if you are part of the game or are the admin.")

        // 4. Get players
        return appUserService.getUsers(game.players.map { player -> player.id})
    }

    @PutMapping("/admin/game")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Create Game", description = "Creates a new game")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "Request successful"),
            ApiResponse(responseCode = "502", description = "An error occurred when attempting to create new game")
    )
    @ResponseBody
    fun put(@RequestBody createGame: CreateGame): Game {
        val subject = SecurityContextHolder.getContext().authentication.name ?: throw ForbiddenException("Couldn't authenticate user")
        val user = appUserService.getUserBySubject(subject)
        return gameService.create(adminId = user.id!!, name= createGame.name, playerIds = createGame.players)
    }

    @PutMapping("/admin/game/cancel")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Cancel a Game", description = "Cancels the game")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "Request successful"),
            ApiResponse(responseCode = "404", description = "Game not found")
    )
    @ResponseBody
    fun cancel(@RequestParam gameId: String) {
        return gameService.cancel(gameId)
    }

    @DeleteMapping("/admin/game")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Delete Game", description = "Delete the game")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "Request successful"),
            ApiResponse(responseCode = "404", description = "Game not found")
    )
    fun delete(@RequestParam gameId: String) {
        return gameService.delete(gameId)
    }

    @PutMapping("/call")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Make a call", description = "After dealing each player can make a call between 0-30")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "Request successful")
    )
    fun call(@RequestParam gameId: String, @RequestParam call: Int) {
        // 1. Get current user ID
        val subject = SecurityContextHolder.getContext().authentication.name ?: throw ForbiddenException("Couldn't authenticate user")
        val user = appUserService.getUserBySubject(subject)

        // 2. Get Game
        val game = gameService.get(gameId)

        // 3. Check the player is in this game
        if (!game.players.map {player -> player.id }.contains(user.id!!)) throw ForbiddenException("Can only call if you are part of the game.")
	
	    // 4. Call
        gameService.call(gameId = game.id, playerId = user.id!!, call = call)
    }

    @PutMapping("/buyCards")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Buy Cards", description = "Buy cards")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "Request successful")
    )
    // TODO: Might need a wrapper object here
    fun buyCards(@RequestParam gameId: String, @RequestBody cards: List<Card>) {
        // 1. Get current user ID
        val subject = SecurityContextHolder.getContext().authentication.name ?: throw ForbiddenException("Couldn't authenticate user")
        val user = appUserService.getUserBySubject(subject)

        // 2. Get Game
        val game = gameService.get(gameId)

        // 3. Check the player is in this game
        if (!game.players.map {player -> player.id }.contains(user.id!!)) throw ForbiddenException("Can only buy cards if you are part of the game.")
	
	    // 4. Buy cards
        gameService.buyCards(gameId = game.id, playerId = user.id!!, selectedCards = cards)
    }

    @PutMapping("/chooseFromDummy")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Choose from the dummy", description = "Choose cards from the dummy")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "Request successful")
    )
    fun chooseFromDummy(@RequestParam gameId: String, @RequestParam suit: Suit, @RequestBody cards: List<Card>) {
        // 1. Get current user ID
        val subject = SecurityContextHolder.getContext().authentication.name ?: throw ForbiddenException("Couldn't authenticate user")
        val user = appUserService.getUserBySubject(subject)

        // 2. Get Game
        val game = gameService.get(gameId)

        // 3. Check the player is in this game
        if (!game.players.map {player -> player.id }.contains(user.id!!)) throw ForbiddenException("Can only choose from the dummy if you are part of the game.")
	
	    // 4. Choose from dummy
        gameService.chooseFromDummy(gameId = game.id, playerId = user.id!!, selectedCards = cards, suit = suit)
    }

    @PutMapping("/playCard")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Play a card", description = "Play a card")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "Request successful")
    )
    fun playCard(@RequestParam gameId: String, @RequestParam card: Card) {
        // 1. Get current user ID
        val subject = SecurityContextHolder.getContext().authentication.name ?: throw ForbiddenException("Couldn't authenticate user")
        val user = appUserService.getUserBySubject(subject)

        // 2. Get Game
        val game = gameService.get(gameId)

        // 3. Check the player is in this game
        if (!game.players.map {player -> player.id }.contains(user.id!!)) throw ForbiddenException("Can only play card if you are part of the game.")
	
	    // 4. Play card
        gameService.playCard(gameId = game.id, playerId = user.id!!, myCard = card)
    }

    @PutMapping("/replay")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Replay the game", description = "Replay the game")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "Request successful")
    )
    fun replay(@RequestParam gameId: String) {
        // 1. Get current user ID
        val subject = SecurityContextHolder.getContext().authentication.name ?: throw ForbiddenException("Couldn't authenticate user")
        val user = appUserService.getUserBySubject(subject)

        // 2. Get Game
        val game = gameService.get(gameId)

        // 3. Check the player is in this game
        if (!game.players.map {player -> player.id }.contains(user.id!!)) throw ForbiddenException("Can only replay game if you are part of the game.")
	
	    // 4. Replay
        gameService.replay(currentGame = game, playerId = user.id!!)
    }

    @GetMapping("/gameState")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Get game", description = "Get the game")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "Request successful")
    )
    @ResponseBody
    fun getGameState(@RequestParam gameId: String): GameState {
        // 1. Get current user ID
        val subject = SecurityContextHolder.getContext().authentication.name ?: throw ForbiddenException("Couldn't authenticate user")
        val user = appUserService.getUserBySubject(subject)

        // 2. Get Game
        val game = gameService.get(gameId)

        // 3. Check the player is in this game
        val me = game.players.find {player -> player.id == user.id!! }

        // 4. If not a player make a spectator
        if (me == null) {
            spectatorService.register(user.id!!, gameId)
            return gameUtils.parseSpectatorGameState(game = game)
        }
	
	    // 5. Get game for player
        return gameUtils.parsePlayerGameState(player = me, game = game)
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java)
    }
}