package ie.daithi.cards.service

interface EmailService {
    fun sendInvite(recipientEmail: String, username: String, password: String, emailMessage: String)
}