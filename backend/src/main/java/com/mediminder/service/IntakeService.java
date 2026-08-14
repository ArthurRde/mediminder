package com.mediminder.service;

import com.mediminder.dto.TodayDtos.IntakeDto;
import com.mediminder.error.ApiException;
import com.mediminder.model.IntakeEvent;
import com.mediminder.model.IntakeStatus;
import com.mediminder.model.User;
import com.mediminder.repository.IntakeEventRepository;
import com.mediminder.repository.MedicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class IntakeService {

    private final IntakeEventRepository eventRepository;
    private final MedicationRepository medicationRepository;
    private final AccessGuard guard;

    public IntakeService(IntakeEventRepository eventRepository,
                         MedicationRepository medicationRepository,
                         AccessGuard guard) {
        this.eventRepository = eventRepository;
        this.medicationRepository = medicationRepository;
        this.guard = guard;
    }

    @Transactional
    public IntakeDto confirm(Long eventId, User user) {
        IntakeEvent event = loadEvent(eventId);
        Long medicationId = event.getSchedule().getMedication().getId();
        guard.requireMember(event.getSchedule().getMedication().getCareCircle().getId(), user);

        int updated = eventRepository.confirmIfOpen(eventId, user, LocalDateTime.now(),
                IntakeStatus.OPEN, IntakeStatus.CONFIRMED);
        if (updated == 0) {
            throw alreadyConfirmed(eventId);
        }
        medicationRepository.decrementStock(medicationId);
        return IntakeDto.from(loadEvent(eventId), false);
    }

    private ApiException alreadyConfirmed(Long eventId) {
        IntakeEvent event = loadEvent(eventId);
        String name = event.getConfirmedBy() == null ? "jemandem" : event.getConfirmedBy().getName();
        String time = event.getConfirmedAt() == null ? "?"
                : event.getConfirmedAt().format(DateTimeFormatter.ofPattern("HH:mm"));
        Map<String, Object> details = new HashMap<>();
        details.put("confirmedBy", name);
        details.put("confirmedAt", event.getConfirmedAt());
        return ApiException.conflict("Bereits von " + name + " um " + time + " bestätigt.", details);
    }

    private IntakeEvent loadEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Einnahme-Event nicht gefunden."));
    }
}
