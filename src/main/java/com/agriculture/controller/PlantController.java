package com.agriculture.controller;

import com.agriculture.dto.AddPlantRequest;
import com.agriculture.models.Plant;
import com.agriculture.models.User;
import com.agriculture.services.PlantService;
import com.agriculture.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/plants")
@CrossOrigin(origins = "*")
public class PlantController {

    private final PlantService plantService;
    private final UserService userService;

    public PlantController(PlantService plantService, UserService userService) {
        this.plantService = plantService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> listUserPlants(@AuthenticationPrincipal User user) {
        List<Plant> plants = plantService.getUserPlants(user.getId());
        return ResponseEntity.ok(plants);
    }

    @DeleteMapping("/{plantId}")
    public ResponseEntity<?> deletePlant(@AuthenticationPrincipal User user, @PathVariable UUID plantId) {
        if (!plantService.doesUserOwnPlant(user.getId(), plantId)) {
            return ResponseEntity.status(403).body("Access denied");
        }
        plantService.deletePlant(plantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<?> addPlant(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddPlantRequest request) {

        try {
            Plant plant = plantService.addPlant(
                    user.getId(),           // UUID userId
                    request.getCulture(),   // String culture
                    request.getName(),      // String name
                    request.getVariety(),   // String variety
                    request.getPlantingDate(), // LocalDate plantingDate
                    request.getGrowthStage(), // String growthStage
                    user.getRegion(),       // String region (из профиля пользователя)
                    user.getGardenType()    // String gardenType (из профиля пользователя)
            );

            return ResponseEntity.ok(plant);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{plantId}/care-plan")
    public ResponseEntity<?> getCarePlan(
            @AuthenticationPrincipal User user,
            @PathVariable UUID plantId) {

        try {
            // Проверяем, что растение принадлежит пользователю
            if (!plantService.doesUserOwnPlant(user.getId(), plantId)) {
                return ResponseEntity.status(403).body("Access denied");
            }

            // Получаем план ухода через сервис
            Object carePlan = plantService.getFeedingSchedule(plantId);
            return ResponseEntity.ok(carePlan);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}