package ie.daithi.cards.service

interface EmailService {
    fun sendInvite(recipientEmail: String, password: String, emailMessage: String)
}