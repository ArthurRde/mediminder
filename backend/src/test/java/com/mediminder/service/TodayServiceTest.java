package com.mediminder.service;

import com.mediminder.IntegrationTestSupport;
import com.mediminder.dto.TodayDtos.TodayResponse;
import com.mediminder.model.CareCircle;
import com.mediminder.model.Role;
import com.mediminder.model.User;
import com.mediminder.repository.IntakeEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TodayServiceTest extends IntegrationTestSupport {

    @Autowired
    private TodayService todayService;
    @Autowired
    private IntakeService intakeService;
    @Autowired
    private IntakeEventRepository eventRepository;

    @Test
    void tagesplanGenerierungIstIdempotent() {
        User sabine = user("Sabine", "sabine@test.de");
        CareCircle circle = circle("Familie Test");
        member(circle, sabine, Role.ADMIN);
        medication(circle, "Ramipril", "5 mg", 10, LocalTime.of(8, 0), LocalTime.of(18, 0));

        TodayResponse first = todayService.getToday(circle.getId(), sabine);
        TodayResponse second = todayService.getToday(circle.getId(), sabine);

        assertEquals(2, first.intakes().size());
        assertEquals(2, second.intakes().size());
        assertEquals(2, eventRepository.count());

        List<Long> firstIds = first.intakes().stream().map(i -> i.id()).toList();
        List<Long> secondIds = second.intakes().stream().map(i -> i.id()).toList();
        assertEquals(firstIds, secondIds);
    }

    @Test
    void intakesSindChronologischSortiert() {
        User sabine = user("Sabine", "sabine2@test.de");
        CareCircle circle = circle("Familie Test");
        member(circle, sabine, Role.ADMIN);
        medication(circle, "Abendmedikament", "10 mg", 5, LocalTime.of(18, 0));
        medication(circle, "Morgenmedikament", "5 mg", 5, LocalTime.of(8, 0));

        TodayResponse response = todayService.getToday(circle.getId(), sabine);

        assertEquals(LocalTime.of(8, 0), response.intakes().get(0).time());
        assertEquals(LocalTime.of(18, 0), response.intakes().get(1).time());
    }

    @Test
    void erzeugtKeineEventsFuerAndereWochentage() {
        User sabine = user("Sabine", "sabine3@test.de");
        CareCircle circle = circle("Familie Test");
        member(circle, sabine, Role.ADMIN);
        var medication = medication(circle, "Wochenmedikament", "5 mg", 5, LocalTime.of(8, 0));
        DayOfWeek notToday = LocalDate.now().getDayOfWeek().plus(1);
        medication.getSchedules().get(0).setDaysOfWeek(EnumSet.of(notToday));
        medicationRepository.save(medication);

        TodayResponse response = todayService.getToday(circle.getId(), sabine);

        assertTrue(response.intakes().isEmpty());
        assertEquals(0, eventRepository.count());
    }

    @Test
    void offeneEventsDeaktivierterMedikamenteVerschwinden() {
        User sabine = user("Sabine", "sabine4@test.de");
        CareCircle circle = circle("Familie Test");
        member(circle, sabine, Role.ADMIN);
        var medication = medication(circle, "Altmedikament", "5 mg", 5, LocalTime.of(8, 0));
        todayService.getToday(circle.getId(), sabine);

        medication.setActive(false);
        medicationRepository.save(medication);

        TodayResponse response = todayService.getToday(circle.getId(), sabine);

        assertTrue(response.intakes().isEmpty());
    }

    @Test
    void bestaetigteEventsDeaktivierterMedikamenteBleibenSichtbar() {
        User sabine = user("Sabine", "sabine5@test.de");
        CareCircle circle = circle("Familie Test");
        member(circle, sabine, Role.ADMIN);
        var medication = medication(circle, "Altmedikament", "5 mg", 5, LocalTime.of(8, 0));
        Long eventId = todayService.getToday(circle.getId(), sabine).intakes().get(0).id();
        intakeService.confirm(eventId, sabine);

        medication.setActive(false);
        medicationRepository.save(medication);

        TodayResponse response = todayService.getToday(circle.getId(), sabine);

        assertEquals(1, response.intakes().size());
        assertEquals("Sabine", response.intakes().get(0).confirmedBy());
    }
}
