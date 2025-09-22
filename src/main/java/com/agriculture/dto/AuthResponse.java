package com.agriculture.dto;

import java.util.UUID;

public class AuthResponse {
    private String token;
    private UUID userId;
    private String email;
    private String name;
    private String region;
    private String gardenType;

    // Конструкторы
    public AuthResponse() {}

    public AuthResponse(String token, UUID userId, String email, String name, String region, String gardenType) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.region = region;
        this.gardenType = gardenType;
    }

    // Геттеры и сеттеры
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getGardenType() { return gardenType; }
    public void setGardenType(String gardenType) { this.gardenType = gardenType; }
}