package ie.daithi.cards.config

import com.sendgrid.SendGrid
import ie.daithi.websockets.model.WsMessage
import io.lettuce.core.RedisURI
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.*
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spi.service.contexts.SecurityContext
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.util.*

import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer

import org.springframework.data.redis.core.RedisTemplate


@Configuration
@ComponentScan(basePackages = ["ie.daithi.cards"])
@EnableSwagger2
class AppConfig(
        @Value("\${sendgrid.api.key}") private val sendgridApiKey: String,
        @Value("\${REDIS_URL}") private val redisUriString: String,
        @Value("\${REDIS_TLS_URL:#{null}}") private val redisTlsUriString: String?) {

    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo())
                .securitySchemes(listOf(securitySchemes()))
                .securityContexts(listOf(securityContexts()))
    }

    private fun securitySchemes(): ApiKey {
        return ApiKey("apiKey", "Authorization", "header")
    }

    private fun securityContexts(): SecurityContext {
        return SecurityContext.builder().securityReferences(defaultAuth()).forPaths(PathSelectors.any()).build()
    }

    private fun defaultAuth(): List<SecurityReference> {
        val authorizationScope = AuthorizationScope("global", "accessEverything")
        val authorizationScopes = arrayOfNulls<AuthorizationScope>(1)
        authorizationScopes[0] = authorizationScope
        return listOf(SecurityReference("apiKey", authorizationScopes))
    }

    private fun apiInfo(): ApiInfo {
        return ApiInfo( "Cards 110 API",
                "A RESTFul API for the Cards 110 application",
                "3.0.0",
                "Whatever",
                Contact("Daithi Hearn","https://github.com/daithihearn", "daithi.hearn@gmail.com"),
                "", "", Collections.emptyList())
    }

    @Bean("emailClient")
    fun emailClient(): SendGrid {
        return SendGrid(sendgridApiKey)
    }

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
            LettuceClientConfiguration.builder().useSsl().build()
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