package com.mediminder.repository;

import com.mediminder.model.Task;
import com.mediminder.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByCareCircleIdOrderByDueDate(Long careCircleId);

    List<Task> findByCareCircleIdAndDueDateOrderByTitle(Long careCircleId, LocalDate dueDate);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Task t set t.assignedTo = :user where t.id = :id and t.assignedTo is null")
    int claimIfUnassigned(@Param("id") Long id, @Param("user") User user);
}
