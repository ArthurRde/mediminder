package com.mediminder.repository;

import com.mediminder.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByCareCircleId(Long careCircleId);
}
