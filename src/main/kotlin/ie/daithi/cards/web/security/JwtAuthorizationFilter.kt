package ie.daithi.cards.web.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.io.IOException
import java.util.Arrays

class JwtAuthorizationFilter(authManager: AuthenticationManager, private val secret: String) : BasicAuthenticationFilter(authManager) {

    @Throws(IOException::class, ServletException::class)
    override fun doFilterInternal(req: HttpServletRequest,
                                            res: HttpServletResponse,
                                            chain: FilterChain) {
        val header = req.getHeader(SecurityConstants.HEADER_STRING)

        if (header == null || !header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            chain.doFilter(req, res)
            return
        }

        val authentication = getAuthentication(req)

        SecurityContextHolder.getContext().authentication = authentication
        chain.doFilter(req, res)
    }

    private fun getAuthentication(request: HttpServletRequest): UsernamePasswordAuthenticationToken? {
        val token = request.getHeader(SecurityConstants.HEADER_STRING)
        if (token != null) {
            // parse the token.
            // TODO: Reading SECRET from a constants file. Need a more secure way of doing this. HSM?
            val jwt = JWT.require(Algorithm.HMAC512(secret.toByteArray()))
                    .build()
                    .verify(token.replace(SecurityConstants.TOKEN_PREFIX, ""))
            val authorities = arrayListOf<SimpleGrantedAuthority>()
            if (jwt != null && jwt.claims != null) {

                jwt.claims.forEach { (key, claim) ->
                    if (key == SecurityConstants.CLAIM_STRING) {
                        val auths = claim.asArray(String::class.java)
                        if (auths != null) {
                            Arrays.stream(auths).forEach { auth -> authorities.add(SimpleGrantedAuthority(auth)) }
                        }
                    }
                }
                return UsernamePasswordAuthenticationToken(jwt.subject, null, authorities)
            }
        }
        return null
    }
}
