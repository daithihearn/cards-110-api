package ie.daithi.cards.web.security

import com.google.common.collect.ImmutableList
import ie.daithi.cards.repositories.mongodb.AppUserRepo
import ie.daithi.cards.service.AppUserService
import ie.daithi.cards.web.security.model.Authority
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@EnableWebSecurity
class WebSecurity (

        private val appUserService: AppUserService,

        private val appUserRepo: AppUserRepo,

        private val bCryptPasswordEncoder: BCryptPasswordEncoder,

        @Value("#{'\${cors.whitelist}'.split(',')}")
        private val allowedOrigins: List<String>,

        @Value("\${security.jwt.secret}")
        private val securitySecret: String,

        @Value("\${security.jwt.expirationTime}")
        private val securityExpirationTime: Long

) : WebSecurityConfigurerAdapter() {

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.cors().and().csrf().disable().authorizeRequests()
                .antMatchers(HttpMethod.POST, "/login").permitAll()
                .antMatchers(HttpMethod.GET, "/swagger-ui.html**").permitAll()
                .antMatchers(HttpMethod.GET, "/webjars/springfox-swagger-ui/**").permitAll()
                .antMatchers(HttpMethod.GET, "/swagger-resources/**").permitAll()
                .antMatchers(HttpMethod.GET, "/websocket/**").permitAll()
                .antMatchers(HttpMethod.GET, "/v2/api-docs").permitAll()
                .antMatchers(HttpMethod.GET, "/static/**").permitAll()
                .antMatchers(HttpMethod.GET, "/assets/**").permitAll()
                .antMatchers(HttpMethod.GET, "/manifest.json").permitAll()
                .antMatchers(HttpMethod.GET, "/index.html").permitAll()
                .antMatchers(HttpMethod.GET, "/#/**").permitAll()
                .antMatchers(HttpMethod.GET, "/").permitAll()
                .antMatchers(HttpMethod.GET, "/favicon.png").permitAll()
                // TODO Add role based rules
//                .antMatchers(HttpMethod.GET, "/api/v1/**").permitAll()
//                .antMatchers(HttpMethod.PUT, "/api/v1/**").permitAll()
//                .antMatchers(HttpMethod.DELETE, "/api/v1/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/v1/admin/**").hasAuthority(Authority.ADMIN.toString())
                .antMatchers(HttpMethod.PUT, "/api/v1/admin/**").hasAuthority(Authority.ADMIN.toString())
                .antMatchers(HttpMethod.DELETE, "/api/v1/admin/**").hasAuthority(Authority.ADMIN.toString())
                .anyRequest().authenticated()
                .and()
                .addFilter(JwtAuthenticationFilter(authenticationManager(), appUserRepo, securitySecret, securityExpirationTime))
                .addFilter(JwtAuthorizationFilter(authenticationManager(), securitySecret))
                // this disables session creation on Spring Security
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    }

    @Throws(Exception::class)
    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(appUserService).passwordEncoder(bCryptPasswordEncoder)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = allowedOrigins
        configuration.allowedMethods = ImmutableList.of("HEAD",
                "GET", "POST", "PUT", "DELETE", "PATCH")
        // setAllowCredentials(true) is important, otherwise:
        // The value of the 'Access-Control-Allow-Origin' header in the response must not be the wildcard '*' when the request's credentials mode is 'include'.
        configuration.allowCredentials = true
        // setAllowedHeaders is important! Without it, OPTIONS preflight request
        // will fail with 403 Invalid CORS request
        configuration.allowedHeaders = ImmutableList.of("Authorization", "Cache-Control", "Content-Type")
        configuration.exposedHeaders = ImmutableList.of("Authorization", "Cache-Control", "Content-Type")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/login", configuration)
        source.registerCorsConfiguration("/api/v1/**", configuration)
        return source
    }

}
