package com.agriculture.controller;

import com.agriculture.dto.PlantVarietyDescriptionRequest;
import com.agriculture.dto.PlantVarietyDescriptionResponse;
import com.agriculture.services.PlantVarietyDescriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/plant-variety")
@CrossOrigin(origins = "*")
public class PlantVarietyDescriptionController {

    @Autowired
    private PlantVarietyDescriptionService varietyDescriptionService;

    /**
     * Получить или создать описание сорта растения
     * Поиск в БД выполняется нечувствительно к регистру
     */
    @PostMapping("/description")
    public ResponseEntity<PlantVarietyDescriptionResponse> getOrCreateVarietyDescription(
            @Valid @RequestBody PlantVarietyDescriptionRequest request) {
        
        try {
            PlantVarietyDescriptionResponse response = varietyDescriptionService.getOrCreateVarietyDescription(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Проверить существование описания сорта
     * Поиск в БД выполняется нечувствительно к регистру
     */
    @GetMapping("/description/exists")
    public ResponseEntity<Boolean> checkVarietyDescriptionExists(
            @RequestParam String culture,
            @RequestParam String variety) {
        
        try {
            boolean exists = varietyDescriptionService.existsVarietyDescription(culture, variety);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Получить описание сорта из базы данных
     * Поиск в БД выполняется нечувствительно к регистру
     */
    @GetMapping("/description")
    public ResponseEntity<PlantVarietyDescriptionResponse> getVarietyDescription(
            @RequestParam String culture,
            @RequestParam String variety) {
        
        try {
            return varietyDescriptionService.getVarietyDescription(culture, variety)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
