package ie.daithi.cards.web.controller

import ie.daithi.cards.enumeration.Card
import ie.daithi.cards.service.DeckService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
@Api(tags = ["Deck"], description = "Endpoints that relate to operations on Decks")
class DeckController(
        private val deckService: DeckService
) {

    @GetMapping("/nextCard")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Get next card", notes = "Get next card")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful")
    )
    @ResponseBody
    fun getActive(@RequestParam deckId: String): Card {
        return deckService.nextCard(deckId)
    }

    @PutMapping("/shuffle")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Create Game", notes = "Shuffles the deck")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful"),
            ApiResponse(code = 502, message = "An error occurred when attempting to shuffle deck")
    )
    fun shuffle(@RequestParam deckId: String) {
        deckService.shuffle(deckId)
    }
}