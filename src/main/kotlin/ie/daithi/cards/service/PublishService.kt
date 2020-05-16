package ie.daithi.cards.service

import com.fasterxml.jackson.databind.ObjectMapper
import ie.daithi.cards.model.PublishContent
import ie.daithi.cards.web.model.enums.EventType
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.web.socket.TextMessage

@Service
class PublishService(
        private val messageSender: SimpMessagingTemplate,
        private val objectMapper: ObjectMapper
) {
    fun publishContent(recipients: List<String>, topic: String, content: Any, gameId: String, contentType: EventType): PublishContent {
        val contentWrapped = PublishContent(gameId = gameId, type = contentType, content = content)
        publishContent(recipients, topic, contentWrapped)
        return contentWrapped
    }

    fun publishContent(recipients: List<String>, topic: String, content: PublishContent): PublishContent {
        val wsMessage = TextMessage(objectMapper.writeValueAsString(content))
        recipients.forEach {recipient ->
            messageSender.convertAndSendToUser(recipient, topic, wsMessage)
        }
        return content
    }

    fun publishContent(recipient: String, topic: String, content: Any, gameId: String, contentType: EventType): PublishContent {
        return publishContent(listOf(recipient), topic, content, gameId, contentType)
    }
}