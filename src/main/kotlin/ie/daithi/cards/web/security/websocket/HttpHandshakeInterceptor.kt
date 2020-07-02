package ie.daithi.cards.web.security.websocket

import ie.daithi.cards.service.AppUserService
import org.springframework.http.server.ServerHttpRequest
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.util.UriUtils
import java.security.Principal

class HttpHandshakeInterceptor(
        private val jwtDecoder: JwtDecoder,
        private val appUserService: AppUserService
): DefaultHandshakeHandler() {

    override fun determineUser(request: ServerHttpRequest, wsHandler: WebSocketHandler, attributes: MutableMap<String, Any>): Principal? {

        val params = UriComponentsBuilder.fromUri(request.uri).build().queryParams
        if (params[TOKEN_ID] != null && params[TOKEN_ID]!!.isNotEmpty()) {
            var tokenId = params[TOKEN_ID]!![0]
            if (null != tokenId && !"null".equals(tokenId, ignoreCase = true)) {
                tokenId = UriUtils.decode(tokenId, "UTF-8")
                val jwt = jwtDecoder.decode(tokenId)

                val user = appUserService.getUserBySubject(jwt.subject)

                val subject = if (params[GAME_ID] != null && params[GAME_ID]!!.isNotEmpty())
                    user.id + params[GAME_ID]!![0]
                else user.id!!

                return StompPrincipal(subject)
            }
        }
        return null
    }

    companion object {
        private const val TOKEN_ID = "tokenId"
        private const val GAME_ID = "gameId"
    }
}