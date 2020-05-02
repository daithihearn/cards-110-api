package ie.daithi.cards.web.security.websocket

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import ie.daithi.cards.web.security.SecurityConstants
import org.springframework.http.server.ServerHttpRequest
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.util.UriUtils
import java.security.Principal

class HttpHandshakeInterceptor(
        private val secret: String?
): DefaultHandshakeHandler() {

    override fun determineUser(request: ServerHttpRequest, wsHandler: WebSocketHandler, attributes: MutableMap<String, Any>): Principal? {

        val params = UriComponentsBuilder.fromUri(request.uri).build().queryParams
        if (params[TOKEN_ID] != null && params[TOKEN_ID]!!.isNotEmpty()) {
            var tokenId = params[TOKEN_ID]!![0]
            if (null != tokenId && !"null".equals(tokenId, ignoreCase = true)) {
                tokenId = UriUtils.decode(tokenId, "UTF-8")
                // parse the token.
                val user = JWT.require(Algorithm.HMAC512(secret!!.toByteArray()))
                        .build()
                        .verify(tokenId.replace(SecurityConstants.TOKEN_PREFIX, ""))
                        .subject
                if (user != null) return StompPrincipal(user)
            }
        }
        return null
    }

    companion object {
        private const val TOKEN_ID = "tokenId"
    }
}