package com.agriculture.services;

import com.agriculture.JsonUtils;
import com.agriculture.models.CarePlan;
import com.agriculture.repository.CarePlanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CarePlanService {

    private final CarePlanRepository carePlanRepository;
    private final DeepSeekService deepSeekService;
    private final ObjectMapper objectMapper;

    public CarePlanService(CarePlanRepository carePlanRepository, DeepSeekService deepSeekService) {
        this.carePlanRepository = carePlanRepository;
        this.deepSeekService = deepSeekService;
        this.objectMapper = new ObjectMapper();
    }

    public boolean existsByParams(String culture, String region, String gardenType) {
        return carePlanRepository.existsByInputParameters(culture, region, gardenType);
    }

    public Optional<JsonNode> getPlanByParams(String culture, String region, String gardenType) {
        return carePlanRepository.findByInputParameters(culture, region, gardenType)
                .map(CarePlan::getAiGeneratedPlan)
                .map(JsonUtils::parseJson);
    }

    /**
     * Основной метод для получения или создания плана ухода с кэшированием
     */
    @Transactional
    public CarePlan getOrCreatePlan(String culture, String region, String gardenType) {
        System.out.println("🔍 Поиск плана ухода для: " + culture + " в регионе " + region + " (" + gardenType + ")");
        
        // 1. Проверяем существование в БД
        Optional<CarePlan> existingPlan = carePlanRepository.findByInputParameters(culture, region, gardenType);
        if (existingPlan.isPresent()) {
            System.out.println("✅ План найден в базе данных");
            return existingPlan.get();
        }

        System.out.println("❌ План не найден в базе данных");

        // 2. Проверяем доступность DeepSeek API
        if (!deepSeekService.isApiAvailable()) {
            System.err.println("🚫 DeepSeek API недоступен");
            throw new RuntimeException("План ухода для культуры '" + culture + 
                "' в регионе '" + region + "' для типа посадки '" + gardenType + 
                "' не найден в базе данных. DeepSeek API недоступен для создания нового плана.");
        }

        System.out.println("🤖 DeepSeek API доступен, запрашиваем новый план...");

        // 3. Запрашиваем у DeepSeek
        JsonNode newPlan = deepSeekService.requestCarePlan(culture, region, gardenType);

        System.out.println("💾 Сохраняем новый план в базу данных...");

        // 4. Сохраняем в БД и возвращаем объект
        CarePlan savedPlan = savePlanToDatabase(culture, region, gardenType, newPlan);
        
        System.out.println("✅ План успешно сохранен с ID: " + savedPlan.getId());
        
        return savedPlan;
    }

    /**
     * Сохраняет план ухода в базу данных
     */
    @Transactional
    public CarePlan savePlanToDatabase(String culture, String region, String gardenType, JsonNode planJson) {
        try {
            // Создаем JSON для input_parameters
            ObjectNode inputParamsJson = objectMapper.createObjectNode();
            inputParamsJson.put("culture", culture);
            inputParamsJson.put("region", region);
            inputParamsJson.put("garden_type", gardenType);

            // Создаем объект CarePlan
            CarePlan carePlan = new CarePlan();
            carePlan.setInputParameters(inputParamsJson.toString());
            carePlan.setAiGeneratedPlan(planJson.toString());

            // Сохраняем в БД
            return carePlanRepository.save(carePlan);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при сохранении плана ухода в базу данных: " + e.getMessage(), e);
        }
    }

    /**
     * Проверяет доступность DeepSeek API
     */
    public boolean isDeepSeekAvailable() {
        return deepSeekService.isApiAvailable();
    }

    /**
     * Получает статистику по планам ухода
     */
    public long getTotalPlansCount() {
        return carePlanRepository.count();
    }

    /**
     * Проверяет существование плана по параметрам
     */
    public boolean planExists(String culture, String region, String gardenType) {
        return existsByParams(culture, region, gardenType);
    }
}


