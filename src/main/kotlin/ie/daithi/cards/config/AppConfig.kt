package ie.daithi.cards.config

import ie.daithi.websockets.model.WsMessage
import io.lettuce.core.RedisURI
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import java.util.*

import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer

import org.springframework.data.redis.core.RedisTemplate

@Configuration
class AppConfig(
        @Value("\${REDIS_URL}") private val redisUriString: String,
        @Value("\${REDIS_TLS_URL:#{null}}") private val redisTlsUriString: String?) {

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {

        val redisUri = if (redisTlsUriString != null)
            RedisURI.create(redisTlsUriString)
        else
            RedisURI.create(redisUriString)

        val config = RedisStandaloneConfiguration()
        config.database = redisUri.database
        config.hostName = redisUri.host
        config.port = redisUri.port
        config.password = RedisPassword.of(redisUri.password)

        val clientConfig = if (redisUri.isSsl)
            LettuceClientConfiguration.builder().useSsl().disablePeerVerification().build()
        else
            LettuceClientConfiguration.builder().build()

        return LettuceConnectionFactory(config, clientConfig)
    }

    @Bean
    fun redisTemplate(redisConnectionFactory: RedisConnectionFactory): RedisTemplate<String, WsMessage> {
        val redisTemplate = RedisTemplate<String, WsMessage>()
        redisTemplate.setConnectionFactory(redisConnectionFactory)
        redisTemplate.setDefaultSerializer(Jackson2JsonRedisSerializer(WsMessage::class.java))
        return redisTemplate
    }
}