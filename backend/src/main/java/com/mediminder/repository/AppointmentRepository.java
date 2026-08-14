package com.mediminder.repository;

import com.mediminder.model.Appointment;
import com.mediminder.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByCareCircleIdAndDateTimeAfterOrderByDateTime(Long careCircleId, LocalDateTime after);

    List<Appointment> findByCareCircleIdAndDateTimeBetweenOrderByDateTime(Long careCircleId,
                                                                          LocalDateTime start,
                                                                          LocalDateTime end);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Appointment a set a.assignedTo = :user where a.id = :id and a.assignedTo is null")
    int claimIfUnassigned(@Param("id") Long id, @Param("user") User user);
}
