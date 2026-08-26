package com.mediminder.service;

import com.mediminder.dto.PlannerDtos.AppointmentDto;
import com.mediminder.dto.PlannerDtos.TaskDto;
import com.mediminder.dto.TodayDtos.IntakeDto;
import com.mediminder.dto.TodayDtos.TodayResponse;
import com.mediminder.model.IntakeEvent;
import com.mediminder.model.IntakeSchedule;
import com.mediminder.model.IntakeStatus;
import com.mediminder.model.User;
import com.mediminder.repository.AppointmentRepository;
import com.mediminder.repository.IntakeEventRepository;
import com.mediminder.repository.IntakeScheduleRepository;
import com.mediminder.repository.TaskRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TodayService {

    private static final int OVERDUE_AFTER = 30;

    private final AccessGuard guard;
    private final IntakeScheduleRepository scheduleRepository;
    private final IntakeEventRepository eventRepository;
    private final AppointmentRepository appointmentRepository;
    private final TaskRepository taskRepository;

    public TodayService(AccessGuard guard,
                        IntakeScheduleRepository scheduleRepository,
                        IntakeEventRepository eventRepository,
                        AppointmentRepository appointmentRepository,
                        TaskRepository taskRepository) {
        this.guard = guard;
        this.scheduleRepository = scheduleRepository;
        this.eventRepository = eventRepository;
        this.appointmentRepository = appointmentRepository;
        this.taskRepository = taskRepository;
    }

    public TodayResponse getToday(Long circleId, User user) {
        guard.requireMember(circleId, user);
        LocalDate today = LocalDate.now();
        generateMissingEvents(circleId, today);
        return new TodayResponse(today, intakes(circleId, today),
                appointments(circleId, today), tasks(circleId, today));
    }

    public void generateMissingEvents(Long circleId, LocalDate date) {
        scheduleRepository.findActiveByCircleId(circleId).stream()
                .filter(schedule -> schedule.getDaysOfWeek().contains(date.getDayOfWeek()))
                .forEach(schedule -> getOrCreateEvent(schedule, date));
    }

    private void getOrCreateEvent(IntakeSchedule schedule, LocalDate date) {
        if (eventRepository.findByScheduleIdAndDate(schedule.getId(), date).isPresent()) {
            return;
        }
        try {
            IntakeEvent event = new IntakeEvent();
            event.setSchedule(schedule);
            event.setDate(date);
            eventRepository.save(event);
        } catch (DataIntegrityViolationException ignored) {
        }
    }

    private List<IntakeDto> intakes(Long circleId, LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        return eventRepository.findByCircleIdAndDate(circleId, date).stream()
                .map(event -> IntakeDto.from(event, isOverdue(event, now)))
                .toList();
    }

    static boolean isOverdue(IntakeEvent event, LocalDateTime now) {
        if (event.getStatus() != IntakeStatus.OPEN) {
            return false;
        }
        LocalDateTime due = event.getDate().atTime(event.getSchedule().getTimeOfDay());
        return now.isAfter(due.plusMinutes(OVERDUE_AFTER));
    }

    private List<AppointmentDto> appointments(Long circleId, LocalDate date) {
        return appointmentRepository
                .findByCareCircleIdAndDateTimeBetweenOrderByDateTime(
                        circleId, date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                .stream()
                .map(AppointmentDto::from)
                .toList();
    }

    private List<TaskDto> tasks(Long circleId, LocalDate date) {
        return taskRepository.findByCareCircleIdAndDueDateOrderByTitle(circleId, date).stream()
                .map(TaskDto::from)
                .toList();
    }
}
