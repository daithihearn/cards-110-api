package ie.daithi.cards.service

interface EmailService {
    fun sendQuizInvite(recipientEmail: String, password: String, emailMessage: String)
}