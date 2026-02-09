package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello (Public)";
    }

    @GetMapping("/secure")
    public String secure() {
        return "You have accessed a protected endpoint using JWT";
    }
}
