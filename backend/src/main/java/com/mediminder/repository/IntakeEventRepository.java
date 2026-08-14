package com.mediminder.repository;

import com.mediminder.model.IntakeEvent;
import com.mediminder.model.IntakeStatus;
import com.mediminder.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IntakeEventRepository extends JpaRepository<IntakeEvent, Long> {

    Optional<IntakeEvent> findByScheduleIdAndDate(Long scheduleId, LocalDate date);

    @Query("""
            select e from IntakeEvent e
            where e.schedule.medication.careCircle.id = :circleId
              and e.date = :date
            order by e.schedule.timeOfDay, e.schedule.medication.name
            """)
    List<IntakeEvent> findByCircleIdAndDate(@Param("circleId") Long circleId, @Param("date") LocalDate date);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update IntakeEvent e
            set e.status = :confirmed, e.confirmedBy = :user, e.confirmedAt = :now
            where e.id = :id and e.status = :open
            """)
    int confirmIfOpen(@Param("id") Long id,
                      @Param("user") User user,
                      @Param("now") LocalDateTime now,
                      @Param("open") IntakeStatus open,
                      @Param("confirmed") IntakeStatus confirmed);
}
