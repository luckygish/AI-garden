package com.agriculture.repository;

import com.agriculture.models.Plant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface PlantRepository extends JpaRepository<Plant, UUID> {

    @Query("SELECT p FROM Plant p WHERE p.user.id = :userId")
    List<Plant> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Plant p WHERE p.id = :plantId AND p.user.id = :userId")
    boolean existsByIdAndUserId(@Param("plantId") UUID plantId, @Param("userId") UUID userId);
}