package ie.daithi.cards.web.controller

import ie.daithi.cards.enumeration.Card
import ie.daithi.cards.service.DeckService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Deck", description = "Endpoints that relate to operations on Decks")
class DeckController(private val deckService: DeckService) {

    @GetMapping("/nextCard")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Get next card", description = "Returns the next card in the deck")
    @ApiResponses(ApiResponse(responseCode = "200", description = "Request successful"))
    @ResponseBody
    fun getActive(@RequestParam deckId: String): Card {
        return deckService.nextCard(deckId)
    }

    @PutMapping("/shuffle")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Shuffle the deck", description = "Shuffles the deck for the given deckId")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Request successful"),
        ApiResponse(
            responseCode = "502",
            description = "An error occurred when attempting to shuffle deck"
        )
    )
    fun shuffle(@RequestParam deckId: String) {
        deckService.shuffle(deckId)
    }
}
