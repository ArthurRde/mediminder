package com.mediminder.web;

import com.mediminder.dto.CircleDtos.*;
import com.mediminder.dto.TodayDtos.TodayResponse;
import com.mediminder.model.User;
import com.mediminder.service.CircleService;
import com.mediminder.service.TodayService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/circles")
public class CircleController {

    private final CircleService circleService;
    private final TodayService todayService;

    public CircleController(CircleService circleService, TodayService todayService) {
        this.circleService = circleService;
        this.todayService = todayService;
    }

    @GetMapping
    public List<CircleSummaryDto> myCircles(@AuthenticationPrincipal User user) {
        return circleService.circlesForUser(user);
    }

    @PostMapping
    public CircleSummaryDto create(@Valid @RequestBody CreateCircleRequest request,
                                   @AuthenticationPrincipal User user) {
        return circleService.create(request.name(), user);
    }

    @GetMapping("/{id}")
    public CircleDetailDto detail(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return circleService.detail(id, user);
    }

    @PutMapping("/{id}/patient")
    public PatientDto upsertPatient(@PathVariable Long id,
                                    @Valid @RequestBody PatientRequest request,
                                    @AuthenticationPrincipal User user) {
        return circleService.upsertPatient(id, request, user);
    }

    @PostMapping("/{id}/invite")
    public InviteDto invite(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return circleService.invite(id, user);
    }

    @PostMapping("/join/{token}")
    public CircleSummaryDto join(@PathVariable String token, @AuthenticationPrincipal User user) {
        return circleService.join(token, user);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public void removeMember(@PathVariable Long id,
                             @PathVariable Long userId,
                             @AuthenticationPrincipal User user) {
        circleService.removeMember(id, userId, user);
    }

    @GetMapping("/{id}/today")
    public TodayResponse today(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return todayService.getToday(id, user);
    }
}
