package com.agriculture.dto;

public class PlantParameters {
    private String culture;
    private String region;
    private String gardenType;

    public PlantParameters() {}

    public PlantParameters(String culture, String region, String gardenType) {
        this.culture = culture;
        this.region = region;
        this.gardenType = gardenType;
    }

    // Геттеры и сеттеры
    public String getCulture() { return culture; }
    public void setCulture(String culture) { this.culture = culture; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getGardenType() { return gardenType; }
    public void setGardenType(String gardenType) { this.gardenType = gardenType; }
}