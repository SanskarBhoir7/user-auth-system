package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/login")
    public String login() {
        return "login";       // login.html
    }

    @GetMapping("/register")
    public String register() {
        return "register";    // register.html
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";   // dashboard.html
    }
}
