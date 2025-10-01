package com.agriculture.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class RootController {

    @GetMapping("/")
    public String home() {
        return "Agriculture Importer Server is running!";
    }

    @GetMapping("/api")
    public String api() {
        return "API is available!";
    }

    @GetMapping("/api/health")
    public String health() {
        return "Server is healthy!";
    }
}
