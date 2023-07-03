package ie.daithi.cards.web.controller

import ie.daithi.cards.service.AppUserService
import ie.daithi.cards.web.exceptions.ForbiddenException
import ie.daithi.cards.model.AppUser
import ie.daithi.cards.web.model.UpdateProfile
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Profile", description = "Endpoints that relate to CRUD operations on Profiles")
class ProfileController(
        private val appUserService: AppUserService
) {

    @GetMapping("/profile/has")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "User has profile?", description = "Does the user have a profile already?")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "Request successful")
    )
    @ResponseBody
    fun hasProfile(): Boolean {
        // 1. Get current user ID
        val subject = SecurityContextHolder.getContext().authentication.name ?: throw ForbiddenException("Couldn't authenticate user")
        val user = appUserService.getUserBySubject(subject)

        // 2. Check if a record exists
        return appUserService.exists(user.id!!)
    }

    @GetMapping("/profile")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Get the profile", description = "Get the user's current profile")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "Request successful")
    )
    @ResponseBody
    fun getProfile(): AppUser {
        // 1. Get current user ID
        val subject = SecurityContextHolder.getContext().authentication.name ?: throw ForbiddenException("Couldn't authenticate user")
        val user = appUserService.getUserBySubject(subject)

        // 2. Check if a record exists
        return appUserService.getUser(user.id!!)
    }

    @PutMapping("/profile")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Update the profile", description = "Update the user's current profile")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "Request successful")
    )
    @ResponseBody
    fun updateProfile(@RequestBody updateProfile: UpdateProfile): AppUser {
        // 1. Get current user ID
        val subject = SecurityContextHolder.getContext().authentication.name ?: throw ForbiddenException("Couldn't authenticate user")

        // 2. Update the profile
        return appUserService.updateUser(subject = subject,
                name = updateProfile.name,
                picture = updateProfile.picture ?: "",
                forceUpdate = updateProfile.forceUpdate)
    }
}