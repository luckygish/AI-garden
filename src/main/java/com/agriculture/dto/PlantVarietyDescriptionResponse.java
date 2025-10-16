package com.agriculture.dto;

import java.time.LocalDateTime;
import java.util.List;

public class PlantVarietyDescriptionResponse {
    
    private String id;
    private String culture;
    private String variety;
    private String description;
    private String ripeningPeriod;
    private String plantHeight;
    private String fruitWeight;
    private String yield;
    private List<String> diseaseResistance;
    private String growingConditions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Конструкторы
    public PlantVarietyDescriptionResponse() {}
    
    // Геттеры и сеттеры
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getCulture() {
        return culture;
    }
    
    public void setCulture(String culture) {
        this.culture = culture;
    }
    
    public String getVariety() {
        return variety;
    }
    
    public void setVariety(String variety) {
        this.variety = variety;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getRipeningPeriod() {
        return ripeningPeriod;
    }
    
    public void setRipeningPeriod(String ripeningPeriod) {
        this.ripeningPeriod = ripeningPeriod;
    }
    
    public String getPlantHeight() {
        return plantHeight;
    }
    
    public void setPlantHeight(String plantHeight) {
        this.plantHeight = plantHeight;
    }
    
    public String getFruitWeight() {
        return fruitWeight;
    }
    
    public void setFruitWeight(String fruitWeight) {
        this.fruitWeight = fruitWeight;
    }
    
    public String getYield() {
        return yield;
    }
    
    public void setYield(String yield) {
        this.yield = yield;
    }
    
    public List<String> getDiseaseResistance() {
        return diseaseResistance;
    }
    
    public void setDiseaseResistance(List<String> diseaseResistance) {
        this.diseaseResistance = diseaseResistance;
    }
    
    public String getGrowingConditions() {
        return growingConditions;
    }
    
    public void setGrowingConditions(String growingConditions) {
        this.growingConditions = growingConditions;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
