package ie.daithi.cards.config.exceptions

import ie.daithi.cards.web.exceptions.ForbiddenException
import ie.daithi.cards.web.exceptions.InvalidEmailException
import ie.daithi.cards.web.exceptions.InvalidOperationException
import ie.daithi.cards.web.exceptions.InvalidStatusException
import ie.daithi.cards.web.exceptions.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.time.LocalDateTime

@ControllerAdvice
class ExceptionHandler {

    @ExceptionHandler(ForbiddenException::class)
    fun generateForbiddenExceptionMessage(ex: ForbiddenException): ResponseEntity<ErrorMessage> {
        val errorMessage = ErrorMessage(timestamp = LocalDateTime.now(),
                error = HttpStatus.FORBIDDEN.reasonPhrase,
                message = ex.message!!,
                status = HttpStatus.FORBIDDEN.value())
        return ResponseEntity(errorMessage, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(InvalidEmailException::class)
    fun generateInvalidEmailExceptionMessage(ex: InvalidEmailException): ResponseEntity<ErrorMessage> {
        val errorMessage = ErrorMessage(timestamp = LocalDateTime.now(),
                error = HttpStatus.BAD_REQUEST.reasonPhrase,
                message = ex.message!!,
                status = HttpStatus.BAD_REQUEST.value())
        return ResponseEntity(errorMessage, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(InvalidOperationException::class)
    fun generateInvalidOperationExceptionMessage(ex: InvalidOperationException): ResponseEntity<ErrorMessage> {
        val errorMessage = ErrorMessage(timestamp = LocalDateTime.now(),
                error = HttpStatus.BAD_REQUEST.reasonPhrase,
                message = ex.message!!,
                status = HttpStatus.BAD_REQUEST.value())
        return ResponseEntity(errorMessage, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(InvalidStatusException::class)
    fun generateInvalidStatusExceptionMessage(ex: InvalidStatusException): ResponseEntity<ErrorMessage> {
        val errorMessage = ErrorMessage(timestamp = LocalDateTime.now(),
                error = HttpStatus.BAD_REQUEST.reasonPhrase,
                message = ex.message!!,
                status = HttpStatus.BAD_REQUEST.value())
        return ResponseEntity(errorMessage, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(NotFoundException::class)
    fun generateNotFoundExceptionMessage(ex: NotFoundException): ResponseEntity<ErrorMessage> {
        val errorMessage = ErrorMessage(timestamp = LocalDateTime.now(),
                error = HttpStatus.NOT_FOUND.reasonPhrase,
                message = ex.message!!,
                status = HttpStatus.NOT_FOUND.value())
        return ResponseEntity(errorMessage, HttpStatus.NOT_FOUND)
    }

}