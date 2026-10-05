package com.campinggearrental.web;

import com.campinggearrental.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.SQLException;

@Controller
public class WebLoginController {
    public static final String AUTHENTICATED_USER_ATTRIBUTE = "authenticatedUser";

    private final AuthService authService;

    public WebLoginController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam(name = "username", required = false) String username,
                        @RequestParam(name = "password", required = false) String password,
                        HttpServletRequest request,
                        Model model) {
        try {
            if (!authService.login(username, password)) {
                model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng.");
                return "login";
            }
        } catch (SQLException exception) {
            model.addAttribute("error", "Không thể đăng nhập lúc này. Vui lòng thử lại sau.");
            return "login";
        }

        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }
        request.getSession(true).setAttribute(AUTHENTICATED_USER_ATTRIBUTE, username.trim());
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        model.addAttribute("username", session.getAttribute(AUTHENTICATED_USER_ATTRIBUTE));
        return "dashboard";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
}