package com.agriculture.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class AddPlantRequest {
    @NotBlank(message = "Культура не может быть пустой")
    private String culture;
    
    @NotBlank(message = "Название растения не может быть пустым")
    private String name;
    
    private String variety;
    
    @NotNull(message = "Дата посадки обязательна")
    private LocalDate plantingDate;
    
    @NotBlank(message = "Стадия роста не может быть пустой")
    private String growthStage;

    public AddPlantRequest() {}

    // Геттеры и сеттеры
    public String getCulture() { return culture; }
    public void setCulture(String culture) { this.culture = culture; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }

    public LocalDate getPlantingDate() { return plantingDate; }
    public void setPlantingDate(LocalDate plantingDate) { this.plantingDate = plantingDate; }

    public String getGrowthStage() { return growthStage; }
    public void setGrowthStage(String growthStage) { this.growthStage = growthStage; }
}