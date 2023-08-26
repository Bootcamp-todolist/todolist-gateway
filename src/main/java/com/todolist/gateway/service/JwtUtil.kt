package com.todolist.gateway.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import org.apache.logging.log4j.util.Strings
import org.springframework.stereotype.Component

@Component
class JwtUtil {
    fun verifyToken(token: String, issuer: String): DecodedJWT {
        val secret = System.getenv("JWT_SECRET")
        if (Strings.isBlank(secret)) {
            throw RuntimeException("Please set JWT_SECRET in env")
        }
        val algorithm = Algorithm.HMAC512(secret)
        val verifier = JWT.require(algorithm).withIssuer(issuer).build()
        return verifier.verify(token)
    }

    fun userId(jwt: DecodedJWT): String {
        return jwt.subject
    }

    fun userRole(jwt: DecodedJWT): String {
        return jwt.getClaim("role").asString()
    }
}
