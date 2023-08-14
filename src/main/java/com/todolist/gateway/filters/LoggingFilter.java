package com.todolist.gateway.filters;

import java.util.UUID;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

  public static final String REQUEST_HEADER_NAME = "X-Request-ID";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String requestId = UUID.randomUUID().toString();
    ThreadContext.put(REQUEST_HEADER_NAME, requestId);
    ServerHttpRequest request = exchange.getRequest().mutate()
        .header(REQUEST_HEADER_NAME, requestId).build();

    return chain.filter(exchange.mutate()
        .request(request)
        .build());
  }

  @Override
  public int getOrder() {
    return 0;
  }
}
