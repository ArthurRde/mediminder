package com.mediminder.dto;

import com.mediminder.model.Appointment;
import com.mediminder.model.Task;
import com.mediminder.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PlannerDtos {

    public record AppointmentRequest(
            @NotBlank(message = "Titel darf nicht leer sein") String title,
            @NotNull(message = "Datum/Uhrzeit fehlt") LocalDateTime dateTime,
            String location) {
    }

    public record AppointmentDto(Long id, String title, LocalDateTime dateTime, String location,
                                 Long assignedToId, String assignedToName) {
        public static AppointmentDto from(Appointment appointment) {
            var assignee = appointment.getAssignedTo();
            return new AppointmentDto(appointment.getId(), appointment.getTitle(), appointment.getDateTime(),
                    appointment.getLocation(),
                    assignee == null ? null : assignee.getId(),
                    assignee == null ? null : assignee.getName());
        }
    }

    public record TaskRequest(
            @NotBlank(message = "Titel darf nicht leer sein") String title,
            @NotNull(message = "Fälligkeitsdatum fehlt") LocalDate dueDate) {
    }

    public record TaskDto(Long id, String title, LocalDate dueDate, TaskStatus status,
                          Long assignedToId, String assignedToName) {
        public static TaskDto from(Task task) {
            var assignee = task.getAssignedTo();
            return new TaskDto(task.getId(), task.getTitle(), task.getDueDate(), task.getStatus(),
                    assignee == null ? null : assignee.getId(),
                    assignee == null ? null : assignee.getName());
        }
    }
}
