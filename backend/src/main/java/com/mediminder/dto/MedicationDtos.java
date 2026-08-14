package com.mediminder.dto;

import com.mediminder.model.IntakeSchedule;
import com.mediminder.model.Medication;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public class MedicationDtos {

    public record ScheduleRequest(
            Long id,
            @NotNull(message = "Uhrzeit fehlt") LocalTime timeOfDay,
            @NotEmpty(message = "Mindestens ein Wochentag nötig") Set<DayOfWeek> daysOfWeek) {
    }

    public record MedicationRequest(
            @NotBlank(message = "Name darf nicht leer sein") String name,
            @NotBlank(message = "Dosierung darf nicht leer sein") String dosage,
            @Min(value = 0, message = "Bestand darf nicht negativ sein") int stockCount,
            @NotEmpty(message = "Mindestens eine Einnahmezeit nötig") @Valid List<ScheduleRequest> schedules) {
    }

    public record ScheduleDto(Long id, LocalTime timeOfDay, Set<DayOfWeek> daysOfWeek, boolean active) {
        public static ScheduleDto from(IntakeSchedule schedule) {
            return new ScheduleDto(schedule.getId(), schedule.getTimeOfDay(),
                    schedule.getDaysOfWeek(), schedule.isActive());
        }
    }

    public record MedicationDto(Long id, String name, String dosage, int stockCount,
                                boolean active, List<ScheduleDto> schedules) {
        public static MedicationDto from(Medication medication) {
            List<ScheduleDto> schedules = medication.getSchedules().stream()
                    .filter(IntakeSchedule::isActive)
                    .map(ScheduleDto::from)
                    .toList();
            return new MedicationDto(medication.getId(), medication.getName(), medication.getDosage(),
                    medication.getStockCount(), medication.isActive(), schedules);
        }
    }
}
