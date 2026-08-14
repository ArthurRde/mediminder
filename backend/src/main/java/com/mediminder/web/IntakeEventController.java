package com.mediminder.web;

import com.mediminder.dto.TodayDtos.IntakeDto;
import com.mediminder.model.User;
import com.mediminder.service.IntakeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/intake-events")
public class IntakeEventController {

    private final IntakeService intakeService;

    public IntakeEventController(IntakeService intakeService) {
        this.intakeService = intakeService;
    }

    @PostMapping("/{id}/confirm")
    public IntakeDto confirm(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return intakeService.confirm(id, user);
    }
}
