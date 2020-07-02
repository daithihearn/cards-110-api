package ie.daithi.cards.web.exceptions

import org.apache.logging.log4j.LogManager
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.FORBIDDEN)
class ForbiddenException: Exception {
    constructor(message: String) : super(message) {
        logger.error(message)
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java)
    }
}