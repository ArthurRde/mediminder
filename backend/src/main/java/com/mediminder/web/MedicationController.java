package com.mediminder.web;

import com.mediminder.dto.MedicationDtos.MedicationDto;
import com.mediminder.dto.MedicationDtos.MedicationRequest;
import com.mediminder.model.User;
import com.mediminder.service.MedicationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/circles/{circleId}/medications")
public class MedicationController {

    private final MedicationService medicationService;

    public MedicationController(MedicationService medicationService) {
        this.medicationService = medicationService;
    }

    @GetMapping
    public List<MedicationDto> list(@PathVariable Long circleId, @AuthenticationPrincipal User user) {
        return medicationService.list(circleId, user);
    }

    @PostMapping
    public MedicationDto create(@PathVariable Long circleId,
                                @Valid @RequestBody MedicationRequest request,
                                @AuthenticationPrincipal User user) {
        return medicationService.create(circleId, request, user);
    }

    @PutMapping("/{medicationId}")
    public MedicationDto update(@PathVariable Long circleId,
                                @PathVariable Long medicationId,
                                @Valid @RequestBody MedicationRequest request,
                                @AuthenticationPrincipal User user) {
        return medicationService.update(circleId, medicationId, request, user);
    }

    @DeleteMapping("/{medicationId}")
    public void deactivate(@PathVariable Long circleId,
                           @PathVariable Long medicationId,
                           @AuthenticationPrincipal User user) {
        medicationService.deactivate(circleId, medicationId, user);
    }
}
