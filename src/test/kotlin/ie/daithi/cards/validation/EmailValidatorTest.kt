package ie.daithi.cards.validation

import org.junit.Assert.*
import org.junit.Test

class EmailValidatorTest {

    @Test
    fun isValid_positive1() {
        val response = emailValidator.isValid("john.doe@email.com")
        assertTrue(response)
    }

    @Test
    fun isValid_positive2() {
        val response = emailValidator.isValid("j0hn.d0e@email.com")
        assertTrue(response)
    }

    @Test
    fun isValid_positive3() {
        val response = emailValidator.isValid("j03r_.doe@email.com")
        assertTrue(response)
    }

    @Test
    fun isValid_positive4() {
        val response = emailValidator.isValid("john.{}x@email.com")
        assertTrue(response)
    }

    @Test
    fun isValid_nagative1() {
        val response = emailValidator.isValid("john.Doe@email.com")
        assertFalse(response)
    }

    @Test
    fun isValid_nagative2() {
        val response = emailValidator.isValid("john.doeemail.com")
        assertFalse(response)
    }

    @Test
    fun isValid_nagative3() {
        val response = emailValidator.isValid("john.doe@emailcom")
        assertFalse(response)
    }

    @Test
    fun isValid_nagative4() {
        val response = emailValidator.isValid("john.doe!@emailcom")
        assertFalse(response)
    }

    companion object {
        private val emailValidator = EmailValidator()
    }

}