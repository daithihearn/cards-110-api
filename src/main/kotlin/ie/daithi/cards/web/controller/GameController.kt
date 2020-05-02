package ie.daithi.cards.web.controller

import ie.daithi.cards.model.Game
import ie.daithi.cards.service.GameService
import ie.daithi.cards.web.model.CreateGame
import io.swagger.annotations.*
import org.apache.logging.log4j.LogManager
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
@Api(tags = ["Game"], description = "Endpoints that relate to CRUD operations on Games")
class GameController (
        private val gameService: GameService
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
        return gameService.create(createGame.name, createGame.playerEmails)
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

    companion object {
        private val logger = LogManager.getLogger(this::class.java)
    }
}