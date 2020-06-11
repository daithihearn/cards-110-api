package ie.daithi.cards.config

import com.sendgrid.SendGrid
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.*
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spi.service.contexts.SecurityContext
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.util.*

@Configuration
@ComponentScan(basePackages = ["ie.daithi.cards"])
@EnableSwagger2
class AppConfig(
        @Value("\${sendgrid.api.key}")
        private val sendgridApiKey: String
) {

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
                "1.0.0",
                "blah",
                Contact("Daithi Hearn","https://github.com/daithihearn", "daithi.hearn@gmail.com"),
                "", "", Collections.emptyList())
    }

    @Bean("emailClient")
    fun emailClient(): SendGrid {
        return SendGrid(sendgridApiKey)
    }
}