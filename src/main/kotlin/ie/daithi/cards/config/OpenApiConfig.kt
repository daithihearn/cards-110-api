package ie.daithi.cards.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    val securityScheme = SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")

    @Bean
    fun openAPI(@Value("\${api.base.path:/}") customServerUrl: String,
                @Value("\${app.version}") appVersion: String): OpenAPI = OpenAPI()
        .info(Info().title("Cards 110 API").version(appVersion).description("The API for the Cards 110 game"))
        .addServersItem(Server().url(customServerUrl).description("Cards API"))
        .addSecurityItem(SecurityRequirement().addList("bearerAuth", emptyList()))
        .components(io.swagger.v3.oas.models.Components().addSecuritySchemes("bearerAuth", securityScheme))
}
