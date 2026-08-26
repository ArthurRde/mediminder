package com.mediminder.service;

import com.mediminder.dto.PlannerDtos.*;
import com.mediminder.error.ApiException;
import com.mediminder.model.Appointment;
import com.mediminder.model.Task;
import com.mediminder.model.TaskStatus;
import com.mediminder.model.User;
import com.mediminder.repository.AppointmentRepository;
import com.mediminder.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PlannerService {

    private final AppointmentRepository appointmentRepository;
    private final TaskRepository taskRepository;
    private final AccessGuard guard;

    public PlannerService(AppointmentRepository appointmentRepository,
                          TaskRepository taskRepository,
                          AccessGuard guard) {
        this.appointmentRepository = appointmentRepository;
        this.taskRepository = taskRepository;
        this.guard = guard;
    }

    public List<AppointmentDto> upcomingAppointments(Long circleId, User user) {
        guard.requireMember(circleId, user);
        return appointmentRepository
                .findByCareCircleIdAndDateTimeAfterOrderByDateTime(circleId, LocalDateTime.now().minusHours(2))
                .stream()
                .map(AppointmentDto::from)
                .toList();
    }

    @Transactional
    public AppointmentDto createAppointment(Long circleId, AppointmentRequest request, User user) {
        var membership = guard.requireMember(circleId, user);
        Appointment appointment = new Appointment();
        appointment.setCareCircle(membership.getCareCircle());
        appointment.setTitle(request.title());
        appointment.setDateTime(request.dateTime());
        appointment.setLocation(request.location());
        return AppointmentDto.from(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDto claimAppointment(Long appointmentId, User user) {
        Appointment appointment = loadAppointment(appointmentId);
        guard.requireMember(appointment.getCareCircle().getId(), user);
        int updated = appointmentRepository.claimIfUnassigned(appointmentId, user);
        if (updated == 0) {
            Appointment fresh = loadAppointment(appointmentId);
            String name = fresh.getAssignedTo().getName();
            throw ApiException.conflict("Schon von " + name + " übernommen.", Map.of("assignedTo", name));
        }
        return AppointmentDto.from(loadAppointment(appointmentId));
    }

    public List<TaskDto> tasks(Long circleId, User user) {
        guard.requireMember(circleId, user);
        return taskRepository.findByCareCircleIdOrderByDueDate(circleId).stream()
                .map(TaskDto::from)
                .toList();
    }

    @Transactional
    public TaskDto createTask(Long circleId, TaskRequest request, User user) {
        var membership = guard.requireMember(circleId, user);
        Task task = new Task();
        task.setCareCircle(membership.getCareCircle());
        task.setTitle(request.title());
        task.setDueDate(request.dueDate());
        return TaskDto.from(taskRepository.save(task));
    }

    @Transactional
    public TaskDto claimTask(Long taskId, User user) {
        Task task = loadTask(taskId);
        guard.requireMember(task.getCareCircle().getId(), user);
        int updated = taskRepository.claimIfUnassigned(taskId, user);
        if (updated == 0) {
            Task fresh = loadTask(taskId);
            String name = fresh.getAssignedTo().getName();
            throw ApiException.conflict("Schon von " + name + " übernommen.", Map.of("assignedTo", name));
        }
        return TaskDto.from(loadTask(taskId));
    }

    @Transactional
    public TaskDto completeTask(Long taskId, User user) {
        Task task = loadTask(taskId);
        guard.requireMember(task.getCareCircle().getId(), user);
        task.setStatus(TaskStatus.DONE);
        if (task.getAssignedTo() == null) {
            task.setAssignedTo(user);
        }
        return TaskDto.from(taskRepository.save(task));
    }

    private Appointment loadAppointment(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Termin nicht gefunden."));
    }

    private Task loadTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Aufgabe nicht gefunden."));
    }
}
