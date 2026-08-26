package com.mediminder.service;

import com.mediminder.IntegrationTestSupport;
import com.mediminder.dto.MedicationDtos.MedicationRequest;
import com.mediminder.dto.MedicationDtos.ScheduleRequest;
import com.mediminder.dto.PlannerDtos.AppointmentRequest;
import com.mediminder.dto.PlannerDtos.TaskRequest;
import com.mediminder.error.ApiException;
import com.mediminder.model.CareCircle;
import com.mediminder.model.Medication;
import com.mediminder.model.Role;
import com.mediminder.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CircleAccessTest extends IntegrationTestSupport {

    @Autowired
    private TodayService todayService;
    @Autowired
    private CircleService circleService;
    @Autowired
    private PlannerService plannerService;
    @Autowired
    private MedicationService medicationService;

    private User sabine;
    private User fremd;
    private CareCircle circle;
    private Medication ramipril;
    private Long appointmentId;
    private Long taskId;

    @BeforeEach
    void setUp() {
        sabine = user("Sabine", "sabine@test.de");
        fremd = user("Fremd", "fremd@test.de");
        circle = circle("Familie Test");
        member(circle, sabine, Role.ADMIN);
        ramipril = medication(circle, "Ramipril", "5 mg", 10, LocalTime.of(8, 0));
        appointmentId = plannerService.createAppointment(circle.getId(),
                new AppointmentRequest("Kardiologe", LocalDateTime.now().plusDays(1), null), sabine).id();
        taskId = plannerService.createTask(circle.getId(),
                new TaskRequest("Rezept anfordern", LocalDate.now()), sabine).id();
    }

    private void erwarte403(org.junit.jupiter.api.function.Executable aufruf) {
        ApiException ex = assertThrows(ApiException.class, aufruf);
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void nichtMitgliedBekommtKeinenTagesplan() {
        erwarte403(() -> todayService.getToday(circle.getId(), fremd));
    }

    @Test
    void nichtMitgliedSiehtKeineKreisDetails() {
        erwarte403(() -> circleService.detail(circle.getId(), fremd));
    }

    @Test
    void nichtMitgliedKannNichtEinladen() {
        erwarte403(() -> circleService.invite(circle.getId(), fremd));
    }

    @Test
    void nichtMitgliedSiehtKeineTermineOderAufgaben() {
        erwarte403(() -> plannerService.upcomingAppointments(circle.getId(), fremd));
        erwarte403(() -> plannerService.tasks(circle.getId(), fremd));
    }

    @Test
    void nichtMitgliedKannNichtsAnlegen() {
        erwarte403(() -> plannerService.createAppointment(circle.getId(),
                new AppointmentRequest("Einbruch", LocalDateTime.now(), null), fremd));
        erwarte403(() -> plannerService.createTask(circle.getId(),
                new TaskRequest("Einbruch", LocalDate.now()), fremd));
    }

    @Test
    void nichtMitgliedKannNichtsUebernehmenOderAbhaken() {
        erwarte403(() -> plannerService.claimAppointment(appointmentId, fremd));
        erwarte403(() -> plannerService.claimTask(taskId, fremd));
        erwarte403(() -> plannerService.completeTask(taskId, fremd));
    }

    @Test
    void nichtMitgliedKannFremdesMedikamentNichtAendern() {
        MedicationRequest request = new MedicationRequest("Ramipril", "10 mg", 5,
                List.of(new ScheduleRequest(null, LocalTime.of(8, 0), EnumSet.allOf(DayOfWeek.class))));
        erwarte403(() -> medicationService.update(circle.getId(), ramipril.getId(), request, fremd));
    }
}
