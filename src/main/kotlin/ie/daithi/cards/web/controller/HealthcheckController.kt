package ie.daithi.cards.web.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
@Api(tags = ["Healthcheck"], description = "Healthcheck endpoints")
class HealthcheckController{

    @GetMapping("/healthcheck")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Get next card", notes = "Get next card")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful")
    )
    @ResponseBody
    fun healthCheck(): String {
        return "OK"
    }
}