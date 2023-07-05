package ie.daithi.cards.web.security.exception

class AuthenticationException(msg: String, t: Throwable) :
    org.springframework.security.core.AuthenticationException(msg, t)
