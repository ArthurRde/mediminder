package com.mediminder;

import com.mediminder.model.*;
import com.mediminder.repository.CareCircleRepository;
import com.mediminder.repository.MedicationRepository;
import com.mediminder.repository.MembershipRepository;
import com.mediminder.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestSupport {

    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected CareCircleRepository circleRepository;
    @Autowired
    protected MembershipRepository membershipRepository;
    @Autowired
    protected MedicationRepository medicationRepository;

    protected User user(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash("egal-fuer-tests");
        return userRepository.save(user);
    }

    protected CareCircle circle(String name) {
        CareCircle circle = new CareCircle();
        circle.setName(name);
        return circleRepository.save(circle);
    }

    protected Membership member(CareCircle circle, User user, Role role) {
        Membership membership = new Membership();
        membership.setCareCircle(circle);
        membership.setUser(user);
        membership.setRole(role);
        return membershipRepository.save(membership);
    }

    protected Medication medication(CareCircle circle, String name, String dosage,
                                    int stock, LocalTime... times) {
        Medication medication = new Medication();
        medication.setCareCircle(circle);
        medication.setName(name);
        medication.setDosage(dosage);
        medication.setStockCount(stock);
        for (LocalTime time : times) {
            IntakeSchedule schedule = new IntakeSchedule();
            schedule.setMedication(medication);
            schedule.setTimeOfDay(time);
            schedule.setDaysOfWeek(EnumSet.allOf(DayOfWeek.class));
            medication.getSchedules().add(schedule);
        }
        return medicationRepository.save(medication);
    }
}
