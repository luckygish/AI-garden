package com.agriculture.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 6, max = 6)
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    private String region;

    @NotBlank
    private String gardenType;

    public RegisterRequest() {}

    public RegisterRequest(String email, String password, String name, String region, String gardenType) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.region = region;
        this.gardenType = gardenType;
    }

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


