package ie.daithi.cards.service

import com.fasterxml.jackson.databind.ObjectMapper
import ie.daithi.cards.model.GameState
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
    fun publishContent(recipients: List<String>, gameState: GameState, contentType: EventType, transitionData: Any? = null): PublishContent {
        val contentWrapped = PublishContent(type = contentType, gameState = gameState, transitionData = transitionData)
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

    fun publishContent(recipient: String, gameState: GameState, contentType: EventType, transitionData: Any? = null): PublishContent {
        return publishContent(listOf(recipient), gameState, contentType, transitionData)
    }

    companion object {
        private val logger = LogManager.getLogger(PublishService::class.java)
    }
}