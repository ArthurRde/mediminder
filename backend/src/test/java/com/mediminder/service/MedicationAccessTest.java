package com.mediminder.service;

import com.mediminder.IntegrationTestSupport;
import com.mediminder.dto.MedicationDtos.MedicationRequest;
import com.mediminder.dto.MedicationDtos.ScheduleRequest;
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
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MedicationAccessTest extends IntegrationTestSupport {

    @Autowired
    private MedicationService medicationService;

    private User sabine;
    private User jonas;
    private CareCircle circle;
    private Medication ramipril;

    @BeforeEach
    void setUp() {
        sabine = user("Sabine", "sabine@test.de");
        jonas = user("Jonas", "jonas@test.de");
        circle = circle("Familie Test");
        member(circle, sabine, Role.ADMIN);
        member(circle, jonas, Role.MEMBER);
        ramipril = medication(circle, "Ramipril", "5 mg", 10, LocalTime.of(8, 0));
    }

    private MedicationRequest request() {
        return new MedicationRequest("Metformin", "500 mg", 25,
                List.of(new ScheduleRequest(null, LocalTime.of(12, 0), EnumSet.allOf(DayOfWeek.class))));
    }

    @Test
    void memberDarfKeinMedikamentAnlegen() {
        ApiException ex = assertThrows(ApiException.class,
                () -> medicationService.create(circle.getId(), request(), jonas));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void memberDarfKeinMedikamentAendern() {
        ApiException ex = assertThrows(ApiException.class,
                () -> medicationService.update(circle.getId(), ramipril.getId(), request(), jonas));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void memberDarfKeinMedikamentDeaktivieren() {
        ApiException ex = assertThrows(ApiException.class,
                () -> medicationService.deactivate(circle.getId(), ramipril.getId(), jonas));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void nichtMitgliedDarfPlanNichtLesen() {
        User fremd = user("Fremd", "fremd@test.de");

        ApiException ex = assertThrows(ApiException.class,
                () -> medicationService.list(circle.getId(), fremd));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void adminDarfMedikamentAnlegen() {
        var dto = medicationService.create(circle.getId(), request(), sabine);

        assertEquals("Metformin", dto.name());
    }
}
