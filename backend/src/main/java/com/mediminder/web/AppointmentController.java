package com.mediminder.web;

import com.mediminder.dto.PlannerDtos.AppointmentDto;
import com.mediminder.dto.PlannerDtos.AppointmentRequest;
import com.mediminder.model.User;
import com.mediminder.service.PlannerService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AppointmentController {

    private final PlannerService plannerService;

    public AppointmentController(PlannerService plannerService) {
        this.plannerService = plannerService;
    }

    @GetMapping("/circles/{circleId}/appointments")
    public List<AppointmentDto> upcoming(@PathVariable Long circleId, @AuthenticationPrincipal User user) {
        return plannerService.upcomingAppointments(circleId, user);
    }

    @PostMapping("/circles/{circleId}/appointments")
    public AppointmentDto create(@PathVariable Long circleId,
                                 @Valid @RequestBody AppointmentRequest request,
                                 @AuthenticationPrincipal User user) {
        return plannerService.createAppointment(circleId, request, user);
    }

    @PostMapping("/appointments/{id}/claim")
    public AppointmentDto claim(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return plannerService.claimAppointment(id, user);
    }
}
