package com.todolist.gateway.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RouterValidatorTest {

  private final RouterValidator routerValidator = new RouterValidator();

  @ParameterizedTest
  @ValueSource(strings = {
      "/api/admin/login",
      "/api/member/login"
  })
  void should_bypass_authentication_on_these_urls(String url) {
    assertFalse(routerValidator.needAuth(url));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "/api/admin/login",

      "/api/admin/register",
      "/api/admin/member",
      "/api/admin/members",
      "/api/admin/member/1",
  })
  void should_do_authentication_on_these_urls(String url) {
    assertTrue(routerValidator.needAuth(url));
  }

}
