package com.mediminder.repository;

import com.mediminder.model.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MedicationRepository extends JpaRepository<Medication, Long> {

    List<Medication> findByCareCircleIdOrderByName(Long careCircleId);

    @Modifying(clearAutomatically = true)
    @Query("update Medication m set m.stockCount = m.stockCount - 1 where m.id = :id and m.stockCount > 0")
    int decrementStock(@Param("id") Long id);
}
