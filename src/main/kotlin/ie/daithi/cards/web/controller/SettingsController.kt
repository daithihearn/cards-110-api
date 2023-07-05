package ie.daithi.cards.web.controller

import ie.daithi.cards.model.PlayerSettings
import ie.daithi.cards.service.SettingsService
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
@Tag(name = "Settings", description = "Endpoints that relate to CRUD operations on Player Settings")
class SettingsController(private val settingsService: SettingsService) {

    @GetMapping("/settings")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Get my settings", description = "Get the settings for the current user")
    @ApiResponses(ApiResponse(responseCode = "200", description = "Request successful"))
    @ResponseBody
    fun getSettings(): PlayerSettings {
        // 1. Get current user ID
        val subject =
            SecurityContextHolder.getContext().authentication.name
                ?: throw ForbiddenException("Couldn't authenticate user")

        // 2. Get settings
        return settingsService.getSettings(subject)
    }

    @PutMapping("/settings")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(
        summary = "Update my settings",
        description = "Update the settings for the current user"
    )
    @ApiResponses(ApiResponse(responseCode = "200", description = "Request successful"))
    @ResponseBody
    fun updateSettings(@RequestBody playerSettings: PlayerSettings): PlayerSettings {
        // 1. Get current user ID
        val subject =
            SecurityContextHolder.getContext().authentication.name
                ?: throw ForbiddenException("Couldn't authenticate user")

        // 2. Set the player ID as the subject
        playerSettings.playerId = subject

        // 3. Update settings
        return settingsService.updateSettings(playerSettings)
    }
}
