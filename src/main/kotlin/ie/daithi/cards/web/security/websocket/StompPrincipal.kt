package ie.daithi.cards.web.security.websocket

import java.security.Principal

class StompPrincipal(
        private var name: String
): Principal {

    override fun getName(): String {
        return name
    }
}