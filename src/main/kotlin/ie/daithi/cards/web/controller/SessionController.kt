package ie.daithi.cards.web.controller

import ie.daithi.cards.repositories.AppUserRepo
import ie.daithi.cards.service.AppUserService
import ie.daithi.cards.service.GameService
import ie.daithi.cards.web.exceptions.NotFoundException
import ie.daithi.cards.web.security.model.Authority
import io.swagger.annotations.*
import org.apache.logging.log4j.LogManager
import org.springframework.http.HttpStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/session")
@Api(tags = ["Session"], description = "Endpoints for session info")
class SessionController (
        private val appUserService: AppUserService,
        private val appUserRepo: AppUserRepo,
        private val gameService: GameService
){

    @GetMapping("/isLoggedIn")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "In the user logged in", notes = "Is the user logged in")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful")
    )
    @ResponseBody
    fun isLoggedIn(): Boolean {
        return true
    }

    @GetMapping("/name")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Get name", notes = "Get logged in user's name")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful")
    )
    @ResponseBody
    fun name(): String {
        val id = SecurityContextHolder.getContext().authentication.name
        val appUser = appUserRepo.findById(id)
        if (appUser.isEmpty)
            throw NotFoundException("User not found")
        if (appUser.get().authorities!!.contains(Authority.ADMIN))
            return appUser.get().username ?: ""
        val game = gameService.getActiveByPlayerId(appUser.get().username!!)
        return gameService.findPlayer(game.players, id).displayName
    }

    @GetMapping("/id")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Get my ID", notes = "Get logged in user's id")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful")
    )
    @ResponseBody
    fun id(): String {
        return SecurityContextHolder.getContext().authentication.name
    }

    @GetMapping("/type")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Get user type", notes = "Get user type")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful")
    )
    @ResponseBody
    fun authorities(): List<GrantedAuthority> {
        val id = SecurityContextHolder.getContext().authentication.name
        logger.debug("Trying to get the user type for $id")

        val appUser = appUserService.loadUserByUsername(id)

        logger.debug("User type: ${appUser.authorities}")
        return appUser.authorities.toMutableList()
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java)
    }
}