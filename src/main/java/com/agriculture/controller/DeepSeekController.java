package com.agriculture.controller;

import com.agriculture.services.DeepSeekService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/deepseek")
@CrossOrigin(origins = "*")
public class DeepSeekController {

    private final DeepSeekService deepSeekService;

    public DeepSeekController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    /**
     * Проверяет доступность DeepSeek API
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("available", deepSeekService.isApiAvailable());
        status.put("message", deepSeekService.isApiAvailable() ? 
            "DeepSeek API настроен и готов к использованию" : 
            "DeepSeek API не настроен. Проверьте переменную окружения DEEPSEEK_API_KEY");
        
        return ResponseEntity.ok(status);
    }

    /**
     * Тестирует запрос к DeepSeek API
     */
    @PostMapping("/test")
    public ResponseEntity<?> testApi(
            @RequestParam String culture,
            @RequestParam String region,
            @RequestParam String gardenType) {
        
        try {
            if (!deepSeekService.isApiAvailable()) {
                return ResponseEntity.badRequest().body("DeepSeek API не настроен");
            }

            JsonNode result = deepSeekService.requestCarePlan(culture, region, gardenType);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Ошибка при тестировании DeepSeek API: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
