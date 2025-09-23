package com.agriculture.controller;

import com.agriculture.JsonUtils;
import com.agriculture.models.CarePlan;
import com.agriculture.services.CarePlanService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/care-plans")
@CrossOrigin(origins = "*")
public class CarePlanController {

    private final CarePlanService carePlanService;

    public CarePlanController(CarePlanService carePlanService) {
        this.carePlanService = carePlanService;
    }

    /**
     * Проверяет существование плана ухода по параметрам
     */
    @GetMapping("/exists")
    public ResponseEntity<?> exists(@RequestParam String culture,
                                    @RequestParam String region,
                                    @RequestParam String gardenType) {
        boolean exists = carePlanService.existsByParams(culture, region, gardenType);
        return ResponseEntity.ok(exists);
    }

    /**
     * Получает план ухода по параметрам (только из БД)
     */
    @GetMapping("/by-params")
    public ResponseEntity<?> getByParams(@RequestParam String culture,
                                         @RequestParam String region,
                                         @RequestParam String gardenType) {
        Optional<JsonNode> planOpt = carePlanService.getPlanByParams(culture, region, gardenType);
        return planOpt.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Получает или создает план ухода с кэшированием
     */
    @GetMapping("/get-or-create")
    public ResponseEntity<?> getOrCreate(@RequestParam String culture,
                                         @RequestParam String region,
                                         @RequestParam String gardenType) {
        try {
            CarePlan carePlan = carePlanService.getOrCreatePlan(culture, region, gardenType);
            // Возвращаем JSON план из объекта CarePlan
            JsonNode planJson = JsonUtils.parseJson(carePlan.getAiGeneratedPlan());
            return ResponseEntity.ok(planJson);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Получает статистику по планам ухода
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPlans", carePlanService.getTotalPlansCount());
        stats.put("deepSeekAvailable", carePlanService.isDeepSeekAvailable());
        return ResponseEntity.ok(stats);
    }

    /**
     * Получает все планы ухода (для отладки)
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllPlans() {
        return ResponseEntity.ok(carePlanService.getAllPlans());
    }

    /**
     * Отладочный endpoint для проверки SQL запроса
     */
    @GetMapping("/debug-query")
    public ResponseEntity<?> debugQuery(@RequestParam String culture,
                                       @RequestParam String region,
                                       @RequestParam String gardenType) {
        return ResponseEntity.ok(carePlanService.debugQuery(culture, region, gardenType));
    }
}


