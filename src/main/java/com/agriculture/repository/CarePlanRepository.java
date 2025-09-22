package com.agriculture.repository;

import com.agriculture.models.CarePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface CarePlanRepository extends JpaRepository<CarePlan, UUID> {

    Optional<CarePlan> findByInputHash(String inputHash);

    @Query("SELECT cp FROM CarePlan cp WHERE cp.inputHash = :hash")
    Optional<CarePlan> findByHash(@Param("hash") String hash);

    boolean existsByInputHash(String inputHash);
}