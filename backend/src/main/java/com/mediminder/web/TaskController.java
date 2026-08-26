package com.mediminder.web;

import com.mediminder.dto.PlannerDtos.TaskDto;
import com.mediminder.dto.PlannerDtos.TaskRequest;
import com.mediminder.model.User;
import com.mediminder.service.PlannerService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final PlannerService plannerService;

    public TaskController(PlannerService plannerService) {
        this.plannerService = plannerService;
    }

    @GetMapping("/circles/{circleId}/tasks")
    public List<TaskDto> list(@PathVariable Long circleId, @AuthenticationPrincipal User user) {
        return plannerService.tasks(circleId, user);
    }

    @PostMapping("/circles/{circleId}/tasks")
    public TaskDto create(@PathVariable Long circleId,
                          @Valid @RequestBody TaskRequest request,
                          @AuthenticationPrincipal User user) {
        return plannerService.createTask(circleId, request, user);
    }

    @PostMapping("/tasks/{id}/claim")
    public TaskDto claim(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return plannerService.claimTask(id, user);
    }

    @PostMapping("/tasks/{id}/done")
    public TaskDto complete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return plannerService.completeTask(id, user);
    }
}
