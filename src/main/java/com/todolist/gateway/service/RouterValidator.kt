package com.todolist.gateway.service

import org.springframework.stereotype.Component

@Component
class RouterValidator {
    fun needAuth(requestPath: String): Boolean {
        return whiteList.stream().noneMatch { regex: String ->
            requestPath.matches(
                regex.toRegex()
            )
        }
    }

    companion object {
        val whiteList = listOf(
            "^/admin/login$",
            "^/member/login$"
        )
    }
}
