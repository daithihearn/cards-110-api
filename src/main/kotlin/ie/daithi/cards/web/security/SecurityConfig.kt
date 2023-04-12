package ie.daithi.cards.web.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


@Configuration
class SecurityConfig(
        @Value("\${auth0.audience}")
        private val audience: String,
        @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
        private val issuer: String,
        @Value("#{'\${cors.whitelist}'.split(',')}")
        private val allowedOrigins: List<String>
) {

    @Bean
    @Throws(Exception::class)
    fun filterChain(http: HttpSecurity): SecurityFilterChain? {
        http.cors().and().authorizeHttpRequests()
            .requestMatchers(HttpMethod.GET, "/swagger-ui/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api-docs/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/actuator/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAuthority("SCOPE_read:game")
            .requestMatchers(HttpMethod.PUT, "/api/v1/**").hasAuthority("SCOPE_write:game")
            .requestMatchers(HttpMethod.GET, "/api/v1/admin/**").hasAuthority("SCOPE_read:admin")
            .requestMatchers(HttpMethod.PUT, "/api/v1/admin/**").hasAuthority("SCOPE_write:admin")
            .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/**").hasAuthority("SCOPE_delete:admin")
            .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer().jwt()
        return http.build()
    }

    @Bean
    fun jwtDecoder(): JwtDecoder? {
        val jwtDecoder = JwtDecoders.fromOidcIssuerLocation(issuer) as NimbusJwtDecoder
        val audienceValidator: OAuth2TokenValidator<Jwt> = AudienceValidator(audience)
        val withIssuer: OAuth2TokenValidator<Jwt> = JwtValidators.createDefaultWithIssuer(issuer)
        val withAudience: OAuth2TokenValidator<Jwt> = DelegatingOAuth2TokenValidator(withIssuer, audienceValidator)
        jwtDecoder.setJwtValidator(withAudience)
        return jwtDecoder
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = allowedOrigins
        configuration.allowedMethods = listOf("HEAD",
                "GET", "POST", "PUT", "DELETE", "PATCH")
        // setAllowCredentials(true) is important, otherwise:
        // The value of the 'Access-Control-Allow-Origin' header in the response must not be the wildcard '*' when the request's credentials mode is 'include'.
        configuration.allowCredentials = true
        // setAllowedHeaders is important! Without it, OPTIONS preflight request
        // will fail with 403 Invalid CORS request
        configuration.allowedHeaders = listOf("Authorization", "Cache-Control", "Content-Type")
        configuration.exposedHeaders = listOf("Authorization", "Cache-Control", "Content-Type")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/api/v1/**", configuration)
        return source
    }

}
