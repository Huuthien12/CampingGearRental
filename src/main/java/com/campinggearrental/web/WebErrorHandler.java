package com.campinggearrental.web;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class WebErrorHandler {
    @ExceptionHandler(Exception.class)
    String error(Exception exception, Model model) { model.addAttribute("message", "Không thể hoàn tất yêu cầu."); return "error"; }
}
