package ie.daithi.cards.web.controller

import ie.daithi.cards.service.AppUserService
import ie.daithi.cards.web.exceptions.ForbiddenException
import ie.daithi.cards.model.AppUser
import ie.daithi.cards.service.GameService
import ie.daithi.cards.service.SpectatorService
import ie.daithi.cards.web.model.UpdateProfile
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
@Api(tags = ["Spectator"], description = "Endpoints that relate to CRUD operations on Spectators")
class SpectatorController(
    private val spectatorService: SpectatorService,
    private val gameService: GameService,
    private val appUserService: AppUserService
) {

    @PutMapping("/spectator/register")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Register as a spectator", notes = "Register as a spectator for a game")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful")
    )
    fun register(@RequestParam gameId: String) {
        // 1. Get current user ID
        val subject = SecurityContextHolder.getContext().authentication.name ?: throw ForbiddenException("Couldn't authenticate user")
        val user = appUserService.getUserBySubject(subject)

        // 2. Get Game
        val game = gameService.get(gameId)

        // 3. Check the player is in this game
        if (game.players.map {player -> player.id }.contains(user.id!!)) throw ForbiddenException("You can't be a spectator in a game you are playing.")

        // 4. Register as a spectator
        spectatorService.register(user.id!!, gameId)
    }
}