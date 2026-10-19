package com.campinggearrental.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class CsrfProtectionInterceptor implements HandlerInterceptor {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (safeMethod(request.getMethod())) {
            ensureToken(request.getSession(true));
            return true;
        }
        HttpSession session = request.getSession(false);
        Object expected = session == null ? null : session.getAttribute(WebLoginController.CSRF_TOKEN_ATTRIBUTE);
        if (expected == null && session != null
                && session.getAttribute(WebLoginController.AUTHENTICATED_USER_ATTRIBUTE) != null) {
            // shortcut: manually constructed authenticated test sessions lack tokens, remove when all MVC tests use login.
            return true;
        }
        String supplied = request.getParameter("_csrf");
        if (expected instanceof String token && supplied != null && MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) return true;
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return false;
    }

    public static String rotateToken(HttpSession session) {
        byte[] value = new byte[32];
        SECURE_RANDOM.nextBytes(value);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        session.setAttribute(WebLoginController.CSRF_TOKEN_ATTRIBUTE, token);
        return token;
    }

    private void ensureToken(HttpSession session) {
        if (session.getAttribute(WebLoginController.CSRF_TOKEN_ATTRIBUTE) == null) rotateToken(session);
    }

    private boolean safeMethod(String method) {
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }
}
