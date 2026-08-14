package com.mediminder.dto;

import com.mediminder.model.IntakeEvent;
import com.mediminder.model.IntakeStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class TodayDtos {

    public record IntakeDto(Long id, LocalTime time, String medicationName, String dosage,
                            IntakeStatus status, String confirmedBy, LocalDateTime confirmedAt,
                            boolean overdue) {
        public static IntakeDto from(IntakeEvent event, boolean overdue) {
            var medication = event.getSchedule().getMedication();
            return new IntakeDto(
                    event.getId(),
                    event.getSchedule().getTimeOfDay(),
                    medication.getName(),
                    medication.getDosage(),
                    event.getStatus(),
                    event.getConfirmedBy() == null ? null : event.getConfirmedBy().getName(),
                    event.getConfirmedAt(),
                    overdue);
        }
    }

    public record TodayResponse(LocalDate date,
                                List<IntakeDto> intakes,
                                List<PlannerDtos.AppointmentDto> appointments,
                                List<PlannerDtos.TaskDto> tasks) {
    }
}
