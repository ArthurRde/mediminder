package com.mediminder.service;

import com.mediminder.model.IntakeEvent;
import com.mediminder.model.IntakeSchedule;
import com.mediminder.model.IntakeStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TodayOverdueTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 26);

    private IntakeEvent eventUm8Uhr(IntakeStatus status) {
        IntakeSchedule schedule = new IntakeSchedule();
        schedule.setTimeOfDay(LocalTime.of(8, 0));
        IntakeEvent event = new IntakeEvent();
        event.setSchedule(schedule);
        event.setDate(DATE);
        event.setStatus(status);
        return event;
    }

    private LocalDateTime um(int stunde, int minute) {
        return DATE.atTime(stunde, minute);
    }

    @Test
    void innerhalbDerKulanzNichtUeberfaellig() {
        assertFalse(TodayService.isOverdue(eventUm8Uhr(IntakeStatus.OPEN), um(8, 29)));
    }

    @Test
    void genauAufDerKulanzgrenzeNichtUeberfaellig() {
        assertFalse(TodayService.isOverdue(eventUm8Uhr(IntakeStatus.OPEN), um(8, 30)));
    }

    @Test
    void nachDerKulanzUeberfaellig() {
        assertTrue(TodayService.isOverdue(eventUm8Uhr(IntakeStatus.OPEN), um(8, 31)));
    }

    @Test
    void vorDerGeplantenZeitNichtUeberfaellig() {
        assertFalse(TodayService.isOverdue(eventUm8Uhr(IntakeStatus.OPEN), um(7, 0)));
    }

    @Test
    void bestaetigteEventsSindNieUeberfaellig() {
        assertFalse(TodayService.isOverdue(eventUm8Uhr(IntakeStatus.CONFIRMED), um(12, 0)));
    }
}
