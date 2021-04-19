package ie.daithi.cards.service

import com.fasterxml.jackson.databind.ObjectMapper
import ie.daithi.cards.model.PublishContent
import ie.daithi.websockets.model.WsMessage
import ie.daithi.cards.web.model.enums.EventType
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.web.socket.TextMessage

@Service
class PublishService(
    private val redisTemplate: RedisTemplate<String, WsMessage>,
    private val objectMapper: ObjectMapper,
    @Value("\${REDIS_TOPIC}") private val topic: String
) {
    fun publishContent(recipients: List<String>, content: Any, contentType: EventType): PublishContent {
        val contentWrapped = PublishContent(type = contentType, content = content)
        publishContent(recipients, contentWrapped)
        return contentWrapped
    }

    fun publishContent(recipients: List<String>, content: PublishContent): PublishContent {
        val wsMessage = TextMessage(objectMapper.writeValueAsString(content))
        recipients.forEach {recipient ->
            redisTemplate.convertAndSend(
                topic,
                WsMessage(recipient = recipient, message = objectMapper.writeValueAsString(wsMessage))
            )
        }
        return content
    }

    fun publishContent(recipient: String, content: Any, contentType: EventType): PublishContent {
        return publishContent(listOf(recipient), content, contentType)
    }

    companion object {
        private val logger = LogManager.getLogger(PublishService::class.java)
    }
}