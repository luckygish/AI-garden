package com.agriculture.services;

import com.agriculture.dto.PlantVarietyDescriptionRequest;
import com.agriculture.dto.PlantVarietyDescriptionResponse;
import com.agriculture.models.PlantVarietyDescription;
import com.agriculture.repository.PlantVarietyDescriptionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PlantVarietyDescriptionService {
    
    @Autowired
    private PlantVarietyDescriptionRepository repository;
    
    @Autowired
    private DeepSeekService deepSeekService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Получить описание сорта из базы данных или создать новое через DeepSeek
     */
    public PlantVarietyDescriptionResponse getOrCreateVarietyDescription(PlantVarietyDescriptionRequest request) {
        String culture = request.getCulture().trim();
        String variety = request.getVariety().trim();
        
        // Сначала проверяем, есть ли описание в базе данных (нечувствительно к регистру)
        Optional<PlantVarietyDescription> existing = repository.findByCultureAndVarietyIgnoreCase(culture, variety);
        
        if (existing.isPresent()) {
            return convertToResponse(existing.get());
        }
        
        // Если описания нет, создаем новое через DeepSeek
        try {
            PlantVarietyDescriptionResponse description = deepSeekService.generateVarietyDescription(request);
            
            // Сохраняем в базу данных
            PlantVarietyDescription entity = convertToEntity(description);
            entity.setCulture(culture.toLowerCase()); // Сохраняем в нижнем регистре для консистентности
            entity.setVariety(variety);
            
            PlantVarietyDescription saved = repository.save(entity);
            return convertToResponse(saved);
            
        } catch (Exception e) {
            throw new RuntimeException("Не удалось получить описание сорта: " + e.getMessage());
        }
    }
    
    /**
     * Проверить существование описания сорта (нечувствительно к регистру)
     */
    public boolean existsVarietyDescription(String culture, String variety) {
        return repository.existsByCultureAndVarietyIgnoreCase(culture.trim(), variety.trim());
    }
    
    /**
     * Получить описание сорта из базы данных (нечувствительно к регистру)
     */
    public Optional<PlantVarietyDescriptionResponse> getVarietyDescription(String culture, String variety) {
        Optional<PlantVarietyDescription> entity = repository.findByCultureAndVarietyIgnoreCase(culture.trim(), variety.trim());
        return entity.map(this::convertToResponse);
    }
    
    /**
     * Конвертировать Entity в Response
     * Названия культуры и сорта форматируются с заглавной буквы
     */
    private PlantVarietyDescriptionResponse convertToResponse(PlantVarietyDescription entity) {
        PlantVarietyDescriptionResponse response = new PlantVarietyDescriptionResponse();
        response.setId(entity.getId().toString());
        response.setCulture(capitalizeFirstLetter(entity.getCulture()));
        response.setVariety(capitalizeFirstLetter(entity.getVariety()));
        response.setDescription(entity.getDescription());
        response.setRipeningPeriod(entity.getRipeningPeriod());
        response.setPlantHeight(entity.getPlantHeight());
        response.setFruitWeight(entity.getFruitWeight());
        response.setYield(entity.getYield());
        response.setGrowingConditions(entity.getGrowingConditions());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        
        // Парсим JSON массив diseaseResistance
        if (entity.getDiseaseResistance() != null && !entity.getDiseaseResistance().isEmpty()) {
            try {
                List<String> diseases = objectMapper.readValue(entity.getDiseaseResistance(), new TypeReference<List<String>>() {});
                response.setDiseaseResistance(diseases);
            } catch (Exception e) {
                response.setDiseaseResistance(new ArrayList<>());
            }
        } else {
            response.setDiseaseResistance(new ArrayList<>());
        }
        
        return response;
    }
    
    /**
     * Конвертировать Response в Entity
     */
    private PlantVarietyDescription convertToEntity(PlantVarietyDescriptionResponse response) {
        PlantVarietyDescription entity = new PlantVarietyDescription();
        entity.setDescription(response.getDescription());
        entity.setRipeningPeriod(response.getRipeningPeriod());
        entity.setPlantHeight(response.getPlantHeight());
        entity.setFruitWeight(response.getFruitWeight());
        entity.setYield(response.getYield());
        entity.setGrowingConditions(response.getGrowingConditions());
        
        // Сохраняем diseaseResistance как JSON
        if (response.getDiseaseResistance() != null && !response.getDiseaseResistance().isEmpty()) {
            try {
                String diseasesJson = objectMapper.writeValueAsString(response.getDiseaseResistance());
                entity.setDiseaseResistance(diseasesJson);
            } catch (Exception e) {
                entity.setDiseaseResistance("[]");
            }
        } else {
            entity.setDiseaseResistance("[]");
        }
        
        return entity;
    }
    
    /**
     * Делает первую букву строки заглавной
     */
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
