package ie.daithi.cards.service

import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.Response
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import ie.daithi.cards.validation.EmailValidator
import ie.daithi.cards.web.exceptions.InvalidEmailException
import ie.daithi.cards.web.exceptions.SendEmailException
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("prod")
class SendGridEmailService(
        private val emailClient: SendGrid,
        private val emailValidator: EmailValidator,
        @Value("\${email.from.address}")
        private val fromAddress: String,
        @Value("\${player.login.url}")
        private val playerLoginUrl: String
): EmailService {

    override fun sendInvite(recipientEmail: String, password: String, emailMessage: String) {
        if(!emailValidator.isValid(recipientEmail))
            throw InvalidEmailException("Invalid email: $recipientEmail")

        val from = Email(fromAddress)
        val subject = "Cards 110"
        val to = Email(recipientEmail)
        val content = Content("text/html", "<html><p>You have been invited to join a game of 110.<br><br>Please click " +
                "<a href='$playerLoginUrl?username=$recipientEmail&password=$password'>$playerLoginUrl?username=$recipientEmail&password=$password</a> to log in</p>" +
                "<p>$emailMessage</p></html>")
        val mail = Mail(from, subject, to, content)

        val request = Request()

        request.method = Method.POST
        request.endpoint = "mail/send"
        request.body = mail.build()
        val response: Response = emailClient.api(request)

        logger.info("Send email operation returned a statusCode of ${response.statusCode}")

        if (response.statusCode > 202)
            throw SendEmailException("An error occurred when attempting to send email to $recipientEmail")
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java)
    }

}