package com.mediminder.seed;

import com.mediminder.model.*;
import com.mediminder.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumSet;

@Component
@Profile("!test")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final CareCircleRepository circleRepository;
    private final MembershipRepository membershipRepository;
    private final PatientRepository patientRepository;
    private final MedicationRepository medicationRepository;
    private final IntakeEventRepository eventRepository;
    private final AppointmentRepository appointmentRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository,
                          CareCircleRepository circleRepository,
                          MembershipRepository membershipRepository,
                          PatientRepository patientRepository,
                          MedicationRepository medicationRepository,
                          IntakeEventRepository eventRepository,
                          AppointmentRepository appointmentRepository,
                          TaskRepository taskRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.circleRepository = circleRepository;
        this.membershipRepository = membershipRepository;
        this.patientRepository = patientRepository;
        this.medicationRepository = medicationRepository;
        this.eventRepository = eventRepository;
        this.appointmentRepository = appointmentRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        User sabine = createUser("Sabine", "sabine@demo.de");
        User jonas = createUser("Jonas", "jonas@demo.de");

        CareCircle circle = new CareCircle();
        circle.setName("Familie Rode");
        circleRepository.save(circle);
        addMembership(circle, sabine, Role.ADMIN);
        addMembership(circle, jonas, Role.MEMBER);

        Patient werner = new Patient();
        werner.setCareCircle(circle);
        werner.setName("Werner");
        werner.setBirthYear(LocalDate.now().getYear() - 81);
        werner.setNote("Wird zuhause von der Familie versorgt.");
        patientRepository.save(werner);

        Medication ramipril = createMedication(circle, "Ramipril", "5 mg", 12,
                LocalTime.of(8, 0), LocalTime.of(18, 0));
        createMedication(circle, "Metformin", "500 mg", 25, LocalTime.of(12, 0));

        confirmMorningDose(ramipril, sabine);
        createKardiologeAppointment(circle);
        createTask(circle);

        log.info("Demo-Daten angelegt: Pflegekreis 'Familie Rode' mit sabine@demo.de / jonas@demo.de (Passwort: demo1234)");
    }

    private User createUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("demo1234"));
        return userRepository.save(user);
    }

    private void addMembership(CareCircle circle, User user, Role role) {
        Membership membership = new Membership();
        membership.setCareCircle(circle);
        membership.setUser(user);
        membership.setRole(role);
        membershipRepository.save(membership);
    }

    private Medication createMedication(CareCircle circle, String name, String dosage,
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

    private void confirmMorningDose(Medication ramipril, User sabine) {
        IntakeSchedule morning = ramipril.getSchedules().stream()
                .filter(s -> s.getTimeOfDay().equals(LocalTime.of(8, 0)))
                .findFirst()
                .orElseThrow();
        IntakeEvent event = new IntakeEvent();
        event.setSchedule(morning);
        event.setDate(LocalDate.now());
        event.setStatus(IntakeStatus.CONFIRMED);
        event.setConfirmedBy(sabine);
        event.setConfirmedAt(LocalDate.now().atTime(8, 2));
        eventRepository.save(event);
    }

    private void createKardiologeAppointment(CareCircle circle) {
        Appointment appointment = new Appointment();
        appointment.setCareCircle(circle);
        appointment.setTitle("Kardiologe");
        appointment.setLocation("Praxis Dr. Sommer");
        appointment.setDateTime(LocalDate.now()
                .with(TemporalAdjusters.next(DayOfWeek.THURSDAY))
                .atTime(9, 30));
        appointmentRepository.save(appointment);
    }

    private void createTask(CareCircle circle) {
        Task task = new Task();
        task.setCareCircle(circle);
        task.setTitle("Rezept anfordern");
        task.setDueDate(LocalDate.now());
        taskRepository.save(task);
    }
}
