package com.mediminder.service;

import com.mediminder.dto.MedicationDtos.MedicationDto;
import com.mediminder.dto.MedicationDtos.MedicationRequest;
import com.mediminder.dto.MedicationDtos.ScheduleRequest;
import com.mediminder.error.ApiException;
import com.mediminder.model.IntakeSchedule;
import com.mediminder.model.Medication;
import com.mediminder.model.User;
import com.mediminder.repository.MedicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Service
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final AccessGuard guard;

    public MedicationService(MedicationRepository medicationRepository, AccessGuard guard) {
        this.medicationRepository = medicationRepository;
        this.guard = guard;
    }

    @Transactional(readOnly = true)
    public List<MedicationDto> list(Long circleId, User user) {
        guard.requireMember(circleId, user);
        return medicationRepository.findByCareCircleIdOrderByName(circleId).stream()
                .filter(Medication::isActive)
                .map(MedicationDto::from)
                .toList();
    }

    @Transactional
    public MedicationDto create(Long circleId, MedicationRequest request, User user) {
        var membership = guard.requireAdmin(circleId, user);
        Medication medication = new Medication();
        medication.setCareCircle(membership.getCareCircle());
        applyRequest(medication, request);
        request.schedules().forEach(s -> medication.getSchedules().add(newSchedule(medication, s)));
        return MedicationDto.from(medicationRepository.save(medication));
    }

    @Transactional
    public MedicationDto update(Long circleId, Long medicationId, MedicationRequest request, User user) {
        guard.requireAdmin(circleId, user);
        Medication medication = loadInCircle(circleId, medicationId);
        applyRequest(medication, request);
        syncSchedules(medication, request.schedules());
        return MedicationDto.from(medicationRepository.save(medication));
    }

    @Transactional
    public void deactivate(Long circleId, Long medicationId, User user) {
        guard.requireAdmin(circleId, user);
        Medication medication = loadInCircle(circleId, medicationId);
        medication.setActive(false);
        medicationRepository.save(medication);
    }

    private void applyRequest(Medication medication, MedicationRequest request) {
        medication.setName(request.name());
        medication.setDosage(request.dosage());
        medication.setStockCount(request.stockCount());
    }

    // Schedules nur deaktivieren statt löschen, alte IntakeEvents referenzieren sie noch
    private void syncSchedules(Medication medication, List<ScheduleRequest> requested) {
        Map<Long, ScheduleRequest> byId = requested.stream()
                .filter(s -> s.id() != null)
                .collect(java.util.stream.Collectors.toMap(ScheduleRequest::id, Function.identity()));
        for (IntakeSchedule existing : medication.getSchedules()) {
            ScheduleRequest match = byId.get(existing.getId());
            if (match == null) {
                existing.setActive(false);
            } else {
                existing.setTimeOfDay(match.timeOfDay());
                existing.setDaysOfWeek(match.daysOfWeek());
                existing.setActive(true);
            }
        }
        requested.stream()
                .filter(s -> s.id() == null)
                .forEach(s -> medication.getSchedules().add(newSchedule(medication, s)));
    }

    private IntakeSchedule newSchedule(Medication medication, ScheduleRequest request) {
        IntakeSchedule schedule = new IntakeSchedule();
        schedule.setMedication(medication);
        schedule.setTimeOfDay(request.timeOfDay());
        schedule.setDaysOfWeek(request.daysOfWeek());
        return schedule;
    }

    private Medication loadInCircle(Long circleId, Long medicationId) {
        return medicationRepository.findById(medicationId)
                .filter(m -> Objects.equals(m.getCareCircle().getId(), circleId))
                .orElseThrow(() -> ApiException.notFound("Medikament nicht gefunden."));
    }
}
