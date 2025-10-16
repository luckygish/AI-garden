package com.agriculture.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "plant_variety_descriptions")
public class PlantVarietyDescription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String culture;
    
    @Column(nullable = false)
    private String variety;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "ripening_period")
    private String ripeningPeriod;
    
    @Column(name = "plant_height")
    private String plantHeight;
    
    @Column(name = "fruit_weight")
    private String fruitWeight;
    
    @Column(name = "yield")
    private String yield;
    
    @Column(name = "disease_resistance", columnDefinition = "TEXT")
    private String diseaseResistance; // JSON array as string
    
    @Column(name = "growing_conditions", columnDefinition = "TEXT")
    private String growingConditions;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Конструкторы
    public PlantVarietyDescription() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public PlantVarietyDescription(String culture, String variety) {
        this();
        this.culture = culture;
        this.variety = variety;
    }
    
    // Геттеры и сеттеры
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
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
    
    public String getDiseaseResistance() {
        return diseaseResistance;
    }
    
    public void setDiseaseResistance(String diseaseResistance) {
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
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
