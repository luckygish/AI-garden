package com.agriculture.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    private String name;
    private String region;
    private String gardenType;

    // Конструкторы
    public AuthRequest() {}

    public AuthRequest(String email, String password, String name, String region, String gardenType) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.region = region;
        this.gardenType = gardenType;
    }

    // Геттеры и сеттеры
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getGardenType() { return gardenType; }
    public void setGardenType(String gardenType) { this.gardenType = gardenType; }
}