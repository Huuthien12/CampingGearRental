package com.campinggearrental.web;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class WebErrorHandler {
    @ExceptionHandler(Exception.class)
    String error(Exception exception, Model model) { model.addAttribute("message", "The request could not be completed."); return "error"; }
}
