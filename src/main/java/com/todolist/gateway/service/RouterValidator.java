package com.todolist.gateway.service;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RouterValidator {

  public static final List<String> whiteList = List.of(
      "^/api/admin/login$",
      "^/api/member/login$"
  );

  public boolean needAuth(String requestPath) {
    return whiteList.stream().noneMatch(requestPath::matches);
  }

}
