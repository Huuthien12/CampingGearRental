package com.campinggearrental.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

public class AuthenticationInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Object authenticatedUser = request.getSession(false) == null
                ? null
                : request.getSession(false).getAttribute(WebLoginController.AUTHENTICATED_USER_ATTRIBUTE);
        if (authenticatedUser instanceof String username && !username.isBlank()) {
            return true;
        }

        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
}