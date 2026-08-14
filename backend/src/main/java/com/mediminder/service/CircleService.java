package com.mediminder.service;

import com.mediminder.dto.CircleDtos.*;
import com.mediminder.error.ApiException;
import com.mediminder.model.*;
import com.mediminder.repository.CareCircleRepository;
import com.mediminder.repository.MembershipRepository;
import com.mediminder.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CircleService {

    private final CareCircleRepository circleRepository;
    private final MembershipRepository membershipRepository;
    private final PatientRepository patientRepository;
    private final AccessGuard guard;

    public CircleService(CareCircleRepository circleRepository,
                         MembershipRepository membershipRepository,
                         PatientRepository patientRepository,
                         AccessGuard guard) {
        this.circleRepository = circleRepository;
        this.membershipRepository = membershipRepository;
        this.patientRepository = patientRepository;
        this.guard = guard;
    }

    public List<CircleSummaryDto> circlesForUser(User user) {
        return membershipRepository.findByUserIdOrderByJoinedAt(user.getId()).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public CircleSummaryDto create(String name, User user) {
        CareCircle circle = new CareCircle();
        circle.setName(name);
        circleRepository.save(circle);
        Membership membership = addMembership(circle, user, Role.ADMIN);
        return toSummary(membership);
    }

    public CircleDetailDto detail(Long circleId, User user) {
        Membership membership = guard.requireMember(circleId, user);
        CareCircle circle = membership.getCareCircle();
        List<MemberDto> members = membershipRepository.findByCareCircleIdOrderByJoinedAt(circleId).stream()
                .map(MemberDto::from)
                .toList();
        PatientDto patient = PatientDto.from(patientRepository.findByCareCircleId(circleId).orElse(null));
        String inviteToken = membership.getRole() == Role.ADMIN ? circle.getInviteToken() : null;
        return new CircleDetailDto(circle.getId(), circle.getName(), membership.getRole(),
                inviteToken, patient, members);
    }

    @Transactional
    public PatientDto upsertPatient(Long circleId, PatientRequest request, User user) {
        Membership membership = guard.requireAdmin(circleId, user);
        Patient patient = patientRepository.findByCareCircleId(circleId).orElseGet(Patient::new);
        patient.setCareCircle(membership.getCareCircle());
        patient.setName(request.name());
        patient.setBirthYear(request.birthYear());
        patient.setNote(request.note());
        return PatientDto.from(patientRepository.save(patient));
    }

    public InviteDto invite(Long circleId, User user) {
        Membership membership = guard.requireAdmin(circleId, user);
        String token = membership.getCareCircle().getInviteToken();
        return new InviteDto(token, "/join/" + token);
    }

    @Transactional
    public CircleSummaryDto join(String token, User user) {
        CareCircle circle = circleRepository.findByInviteToken(token)
                .orElseThrow(() -> ApiException.notFound("Dieser Einladungslink ist ungültig."));
        Membership membership = membershipRepository
                .findByUserIdAndCareCircleId(user.getId(), circle.getId())
                .orElseGet(() -> addMembership(circle, user, Role.MEMBER));
        return toSummary(membership);
    }

    @Transactional
    public void removeMember(Long circleId, Long userId, User caller) {
        guard.requireAdmin(circleId, caller);
        if (caller.getId().equals(userId)) {
            throw ApiException.badRequest("Als Admin kannst du dich nicht selbst entfernen.");
        }
        Membership membership = membershipRepository.findByUserIdAndCareCircleId(userId, circleId)
                .orElseThrow(() -> ApiException.notFound("Dieses Mitglied gibt es hier nicht."));
        membershipRepository.delete(membership);
    }

    private Membership addMembership(CareCircle circle, User user, Role role) {
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setCareCircle(circle);
        membership.setRole(role);
        return membershipRepository.save(membership);
    }

    private CircleSummaryDto toSummary(Membership membership) {
        CareCircle circle = membership.getCareCircle();
        String patientName = patientRepository.findByCareCircleId(circle.getId())
                .map(Patient::getName)
                .orElse(null);
        return new CircleSummaryDto(circle.getId(), circle.getName(), membership.getRole(), patientName);
    }
}
