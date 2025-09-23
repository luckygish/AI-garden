package com.agriculture.repository;

import com.agriculture.models.CarePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface CarePlanRepository extends JpaRepository<CarePlan, UUID> {

    @Query(value = "SELECT * FROM care_plans WHERE " +
           "input_parameters->>'culture' = :culture AND " +
           "input_parameters->>'region' = :region AND " +
           "input_parameters->>'garden_type' = :gardenType", 
           nativeQuery = true)
    Optional<CarePlan> findByInputParameters(@Param("culture") String culture, 
                                           @Param("region") String region, 
                                           @Param("gardenType") String gardenType);

    @Query(value = "SELECT * FROM care_plans", nativeQuery = true)
    java.util.List<CarePlan> findAllPlans();

    @Query(value = "SELECT COUNT(*) > 0 FROM care_plans WHERE " +
           "input_parameters->>'culture' = :culture AND " +
           "input_parameters->>'region' = :region AND " +
           "input_parameters->>'garden_type' = :gardenType", 
           nativeQuery = true)
    boolean existsByInputParameters(@Param("culture") String culture, 
                                  @Param("region") String region, 
                                  @Param("gardenType") String gardenType);
}