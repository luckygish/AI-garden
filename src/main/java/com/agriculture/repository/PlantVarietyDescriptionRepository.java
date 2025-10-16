package com.agriculture.repository;

import com.agriculture.models.PlantVarietyDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlantVarietyDescriptionRepository extends JpaRepository<PlantVarietyDescription, UUID> {
    
    /**
     * Найти описание сорта по культуре и сорту (нечувствительно к регистру)
     */
    @Query("SELECT p FROM PlantVarietyDescription p WHERE LOWER(p.culture) = LOWER(:culture) AND LOWER(p.variety) = LOWER(:variety)")
    Optional<PlantVarietyDescription> findByCultureAndVarietyIgnoreCase(@Param("culture") String culture, @Param("variety") String variety);
    
    /**
     * Проверить существование описания для сорта (нечувствительно к регистру)
     */
    @Query("SELECT COUNT(p) > 0 FROM PlantVarietyDescription p WHERE LOWER(p.culture) = LOWER(:culture) AND LOWER(p.variety) = LOWER(:variety)")
    boolean existsByCultureAndVarietyIgnoreCase(@Param("culture") String culture, @Param("variety") String variety);
}
