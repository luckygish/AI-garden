package com.agriculture.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Email
    @NotBlank
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    private String name;

    @NotBlank
    @Column(nullable = false)
    private String region;

    @NotBlank
    @Column(name = "garden_type", nullable = false)
    private String gardenType;

    // Конструкторы
    public User() {} // Конструктор по умолчанию (обязателен для JPA)

    public User(UUID id) { // Конструктор только с ID
        this.id = id;
    }

    public User(String email, String password, String region, String gardenType, String name) {
        this.email = email;
        this.password = password;
        this.region = region;
        this.gardenType = gardenType;
        this.name = name;
    }

    // Геттеры и сеттеры
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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