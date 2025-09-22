package com.agriculture.models;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "care_plans")
public class CarePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "input_hash", unique = true, nullable = false)
    private String inputHash;

    @Column(name = "input_parameters", columnDefinition = "JSONB")
    private String inputParameters;

    @Column(name = "ai_generated_plan", columnDefinition = "JSONB")
    private String aiGeneratedPlan;

    public CarePlan() {}

    // Геттеры и сеттеры
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getInputHash() { return inputHash; }
    public void setInputHash(String inputHash) { this.inputHash = inputHash; }

    public String getInputParameters() { return inputParameters; }
    public void setInputParameters(String inputParameters) { this.inputParameters = inputParameters; }

    public String getAiGeneratedPlan() { return aiGeneratedPlan; }
    public void setAiGeneratedPlan(String aiGeneratedPlan) { this.aiGeneratedPlan = aiGeneratedPlan; }
}