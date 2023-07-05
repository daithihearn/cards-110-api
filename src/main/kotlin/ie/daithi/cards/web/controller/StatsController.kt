package ie.daithi.cards.web.controller

import ie.daithi.cards.model.PlayerGameStats
import ie.daithi.cards.service.AppUserService
import ie.daithi.cards.service.StatsService
import ie.daithi.cards.web.exceptions.ForbiddenException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Statistics", description = "Endpoints that relate to game statistics")
class StatsController(
    private val appUserService: AppUserService,
    private val statsService: StatsService
) {

    @GetMapping("/stats/gameStatsForPlayer")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(
        summary = "Game stats for current player",
        description = "Returns the game stats for the current player"
    )
    @ApiResponses(ApiResponse(responseCode = "200", description = "Request successful"))
    @ResponseBody
    fun gameStatsForPlayer(): List<PlayerGameStats> {
        // 1. Get current user ID
        val subject =
            SecurityContextHolder.getContext().authentication.name
                ?: throw ForbiddenException("Couldn't authenticate user")
        val user = appUserService.getUserBySubject(subject)

        return statsService.gameStatsForPlayer(user.id!!)
    }

    @GetMapping("/admin/stats/gameStatsForPlayer")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(
        summary = "Game stats for given player player",
        description = "Returns the game stats for the given player"
    )
    @ApiResponses(ApiResponse(responseCode = "200", description = "Request successful"))
    @ResponseBody
    fun gameStatsForPlayer(@RequestParam playerId: String): List<PlayerGameStats> {
        return statsService.gameStatsForPlayer(playerId)
    }
}
