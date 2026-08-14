package com.mediminder.service;

import com.mediminder.IntegrationTestSupport;
import com.mediminder.dto.TodayDtos.IntakeDto;
import com.mediminder.error.ApiException;
import com.mediminder.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class IntakeConfirmTest extends IntegrationTestSupport {

    @Autowired
    private TodayService todayService;
    @Autowired
    private IntakeService intakeService;

    private User sabine;
    private User jonas;
    private CareCircle circle;
    private Medication ramipril;
    private Long eventId;

    @BeforeEach
    void setUp() {
        sabine = user("Sabine", "sabine@test.de");
        jonas = user("Jonas", "jonas@test.de");
        circle = circle("Familie Test");
        member(circle, sabine, Role.ADMIN);
        member(circle, jonas, Role.MEMBER);
        ramipril = medication(circle, "Ramipril", "5 mg", 10, LocalTime.of(8, 0));
        eventId = todayService.getToday(circle.getId(), sabine).intakes().get(0).id();
    }

    @Test
    void ersterConfirmBestaetigtUndReduziertBestand() {
        IntakeDto result = intakeService.confirm(eventId, sabine);

        assertEquals(IntakeStatus.CONFIRMED, result.status());
        assertEquals("Sabine", result.confirmedBy());
        assertNotNull(result.confirmedAt());
        assertEquals(9, medicationRepository.findById(ramipril.getId()).orElseThrow().getStockCount());
    }

    @Test
    void zweiterConfirmLiefertKonfliktMitBestaetiger() {
        intakeService.confirm(eventId, sabine);

        ApiException ex = assertThrows(ApiException.class, () -> intakeService.confirm(eventId, jonas));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Sabine", ex.getDetails().get("confirmedBy"));
        assertNotNull(ex.getDetails().get("confirmedAt"));
        assertEquals(9, medicationRepository.findById(ramipril.getId()).orElseThrow().getStockCount());
    }

    @Test
    void nichtMitgliedDarfNichtBestaetigen() {
        User fremd = user("Fremd", "fremd@test.de");

        ApiException ex = assertThrows(ApiException.class, () -> intakeService.confirm(eventId, fremd));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void bestandFaelltNichtUnterNull() {
        ramipril.setStockCount(0);
        medicationRepository.save(ramipril);

        intakeService.confirm(eventId, sabine);

        assertEquals(0, medicationRepository.findById(ramipril.getId()).orElseThrow().getStockCount());
    }
}
