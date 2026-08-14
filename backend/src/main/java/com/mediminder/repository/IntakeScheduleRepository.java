package com.mediminder.repository;

import com.mediminder.model.IntakeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IntakeScheduleRepository extends JpaRepository<IntakeSchedule, Long> {

    @Query("""
            select s from IntakeSchedule s
            where s.medication.careCircle.id = :circleId
              and s.active = true
              and s.medication.active = true
            """)
    List<IntakeSchedule> findActiveByCircleId(@Param("circleId") Long circleId);
}
