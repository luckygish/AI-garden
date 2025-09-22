package com.agriculture.services;

import com.agriculture.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.agriculture.dto.PlantParameters;
import com.agriculture.dto.FeedingScheduleResponse;
import com.agriculture.models.Plant;
import com.agriculture.models.CarePlan;
import com.agriculture.models.User;
import com.agriculture.repository.PlantRepository;
import com.agriculture.repository.CarePlanRepository;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PlantService {

    private final PlantRepository plantRepository;
    private final CarePlanRepository carePlanRepository;

    public PlantService(PlantRepository plantRepository, CarePlanRepository carePlanRepository) {
        this.plantRepository = plantRepository;
        this.carePlanRepository = carePlanRepository;
    }

    @Transactional
    public Plant addPlant(UUID userId, String culture, String name, String variety,
                          LocalDate plantingDate, String growthStage, String region, String gardenType) {

        // Генерируем хэш на основе параметров
        PlantParameters params = new PlantParameters(culture, region, gardenType);
        String inputHash = generateInputHash(params);

        // Ищем план в БД
        CarePlan carePlan = carePlanRepository.findByInputHash(inputHash)
                .orElseThrow(() -> new RuntimeException("План ухода не найден для параметров: " +
                        culture + ", " + region + ", " + gardenType));

        // Создаем растение
        Plant plant = new Plant();
        plant.setUser(new User(userId));
        plant.setCarePlan(carePlan);
        plant.setName(name);
        plant.setVariety(variety);
        plant.setPlantingDate(plantingDate);
        plant.setGrowthStage(growthStage);

        return plantRepository.save(plant);
    }

    public List<Plant> getUserPlants(UUID userId) {
        return plantRepository.findByUserId(userId);
    }

    public Plant getPlantById(UUID plantId) {
        return plantRepository.findById(plantId)
                .orElseThrow(() -> new RuntimeException("Растение не найдено"));
    }

    @Transactional
    public void deletePlant(UUID plantId) {
        if (!plantRepository.existsById(plantId)) {
            throw new RuntimeException("Растение не найдено");
        }
        plantRepository.deleteById(plantId);
    }

    public FeedingScheduleResponse getFeedingSchedule(UUID plantId) {
        Plant plant = getPlantById(plantId);

        try {
            // Извлекаем данные из JSONB плана ухода
            JsonNode planJson = JsonUtils.parseJson(plant.getCarePlan().getAiGeneratedPlan());
            JsonNode operations = planJson.get("operations");

            List<FeedingScheduleResponse.FeedingScheduleItem> schedule = new java.util.ArrayList<>();

            for (JsonNode operation : operations) {
                if ("подкормка".equals(operation.get("type").asText())) {
                    JsonNode materials = operation.get("materials");
                    if (materials != null && materials.isArray() && materials.size() > 0) {
                        JsonNode firstMaterial = materials.get(0);

                        FeedingScheduleResponse.FeedingScheduleItem item =
                                new FeedingScheduleResponse.FeedingScheduleItem(
                                        operation.get("period").asText(),
                                        operation.get("fase").asText(),
                                        firstMaterial.get("name").asText(),
                                        firstMaterial.get("method").asText()
                                );
                        schedule.add(item);
                    }
                }
            }

            return new FeedingScheduleResponse(plant.getName(), schedule);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении графика подкормок", e);
        }
    }

    private String generateInputHash(PlantParameters params) {
        String normalizedString = String.format("culture:%s|region:%s|area:%s",
                params.getCulture().toLowerCase().trim(),
                params.getRegion().toLowerCase().trim(),
                params.getGardenType().toLowerCase().trim());

        return DigestUtils.md5Hex(normalizedString);
    }

    public boolean doesUserOwnPlant(UUID userId, UUID plantId) {
        return plantRepository.existsByIdAndUserId(plantId, userId);
    }
}