package com.todolist.gateway.filters

import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.todolist.gateway.common.Constant.USER_ID
import com.todolist.gateway.common.Constant.USER_ROLE
import com.todolist.gateway.common.Role
import com.todolist.gateway.service.JwtUtil
import com.todolist.gateway.service.RouterValidator
import lombok.RequiredArgsConstructor
import mu.KotlinLogging
import org.apache.logging.log4j.ThreadContext
import org.apache.logging.log4j.util.Strings
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
@RequiredArgsConstructor
@Order(-1)
class AuthenticationFilter(private val routerValidator: RouterValidator,
                           private val jwtUtil: JwtUtil) : GlobalFilter {
    private val log = KotlinLogging.logger {}

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val requestPath = request.uri.path
        val decodedJWT: DecodedJWT
        if (routerValidator.needAuth(requestPath)) {
            try {
                decodedJWT = validateToken(request)
            } catch (e: AuthError) {
                return onError(exchange, e.message)
            }
            val userId = jwtUtil.userId(decodedJWT)
            val userRole = jwtUtil.userRole(decodedJWT)
            if (!(userRole == Role.ADMIN.toString() && requestPath.startsWith("/admin/")) ||
                userRole != Role.MEMBER.toString() && requestPath.startsWith("/member/")
            ) {
                return onError(exchange, "Access denied")
            }
            populateRequestWithHeaders(exchange, userId, userRole)
        }
        addResponseHeader(exchange.response)
        return chain.filter(exchange)
    }

    private fun validateToken(request: ServerHttpRequest): DecodedJWT {
        val token = getAuthHeader(request)
        val decodedJWT: DecodedJWT
        if (Strings.isBlank(token)) {
            throw AuthError("Authorization is missing in request")
        }
        requireNotNull(token) {
        }
        try {
            decodedJWT = jwtUtil.verifyToken(token, TODO_LIST)
            return decodedJWT
        } catch (e: JWTVerificationException) {
            log.error("Token is invalid: ", e)
            throw AuthError("Authorization token is invalid")
        }
    }

    private fun onError(exchange: ServerWebExchange, err: String?): Mono<Void> {
        log.error("Authorization failed: {}", err)
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        addResponseHeader(response)
        return response.setComplete()
    }

    private fun addResponseHeader(response: ServerHttpResponse) {
        response.headers.add(
            REQUEST_HEADER_NAME, ThreadContext.get(REQUEST_HEADER_NAME)
        )
    }

    private fun getAuthHeader(request: ServerHttpRequest): String? {
        return try {
            request.headers.getOrEmpty("Authorization")[0]
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }

    private fun populateRequestWithHeaders(
        exchange: ServerWebExchange, userId: String, userRole: String
    ) {
        exchange.request.mutate()
            .header(USER_ID, userId)
            .header(USER_ROLE, userRole)
            .build()
    }

    internal class AuthError(message: String) : RuntimeException(message)

    companion object {
        const val REQUEST_HEADER_NAME = "X-Request-ID"
        const val TODO_LIST = "todo-list"
    }
}
