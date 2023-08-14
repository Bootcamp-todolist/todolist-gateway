package com.todolist.gateway.filters;

import static com.todolist.gateway.common.Constant.USER_ID;
import static com.todolist.gateway.common.Constant.USER_ROLE;
import static com.todolist.gateway.common.Role.ADMIN;
import static com.todolist.gateway.common.Role.USER;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.todolist.gateway.service.JwtUtil;
import com.todolist.gateway.service.RouterValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.util.Strings;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFilter implements GlobalFilter, Ordered {

  private final RouterValidator routerValidator;
  private final JwtUtil jwtUtil;

  public static final String REQUEST_HEADER_NAME = "X-Request-ID";
  public static final String TODO_LIST = "todo-list";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String requestPath = request.getURI().getPath();
    DecodedJWT decodedJWT;
    if (routerValidator.needAuth(requestPath)) {
      try {
        decodedJWT = validateToken(request);
      } catch (AuthError e) {
        return this.onError(exchange, e.getMessage());
      }

      String userId = jwtUtil.userId(decodedJWT);
      String userRole = jwtUtil.userRole(decodedJWT);
      //FIXME
      if (!(userRole.equals(ADMIN.toString()) && requestPath.startsWith("/api/admin/")) || (
          !userRole.equals(USER.toString()) && requestPath.startsWith("/api/member/"))) {
        return this.onError(exchange, "Access denied");
      }
      this.populateRequestWithHeaders(exchange, userId, userRole);
    }
    addResponseHeader(exchange.getResponse());
    return chain.filter(exchange);
  }

  private DecodedJWT validateToken(ServerHttpRequest request) {
    String token = getAuthHeader(request);
    DecodedJWT decodedJWT;

    if (Strings.isBlank(token)) {
      throw new AuthError("Authorization is missing in request");
    }

    try {
      decodedJWT = jwtUtil.verifyToken(token, TODO_LIST);
      return decodedJWT;
    } catch (JWTVerificationException e) {
      log.error("Token is invalid: ", e);
      throw new AuthError("Authorization token is invalid");
    }
  }

  private Mono<Void> onError(ServerWebExchange exchange, String err) {
    log.error("Authorization failed: {}", err);
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    addResponseHeader(response);
    return response.setComplete();
  }

  private void addResponseHeader(ServerHttpResponse response) {
    response.getHeaders().add(REQUEST_HEADER_NAME, ThreadContext.get(REQUEST_HEADER_NAME));
  }

  private String getAuthHeader(ServerHttpRequest request) {
    try {
      return request.getHeaders().getOrEmpty("Authorization").get(0);
    } catch (IndexOutOfBoundsException e) {
      return null;
    }
  }

  private void populateRequestWithHeaders(
      ServerWebExchange exchange, String userId, String userRole) {
    exchange.getRequest().mutate()
        .header(USER_ID, userId)
        .header(USER_ROLE, userRole)
        .build();
  }

  @Override
  public int getOrder() {
    return 1;
  }

  static class AuthError extends RuntimeException {

    public AuthError(String message) {
      super(message);
    }
  }

}
