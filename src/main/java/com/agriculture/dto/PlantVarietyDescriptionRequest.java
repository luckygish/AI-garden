package com.agriculture.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PlantVarietyDescriptionRequest {
    
    @NotBlank(message = "Культура обязательна")
    private String culture;
    
    @NotBlank(message = "Сорт обязателен")
    @Size(max = 100, message = "Название сорта не должно превышать 100 символов")
    private String variety;
    
    // Конструкторы
    public PlantVarietyDescriptionRequest() {}
    
    public PlantVarietyDescriptionRequest(String culture, String variety) {
        this.culture = culture;
        this.variety = variety;
    }
    
    // Геттеры и сеттеры
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
}
