package com.todolist.gateway.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtUtil {

  public DecodedJWT verifyToken(String token, String issuer) {
    String secret = System.getenv("JWT_SECRET");
    if (Strings.isBlank(secret)) {
      throw new RuntimeException("Please set JWT_SECRET in env");
    }
    Algorithm algorithm = Algorithm.HMAC512(secret);
    JWTVerifier verifier = JWT.require(algorithm).withIssuer(issuer).build();
    return verifier.verify(token);
  }

  public String userId(DecodedJWT jwt) {
    return jwt.getSubject();
  }

  public String userRole(DecodedJWT jwt) {
    return jwt.getClaim("role").asString();
  }
}
