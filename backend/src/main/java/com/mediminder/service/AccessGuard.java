package com.mediminder.service;

import com.mediminder.error.ApiException;
import com.mediminder.model.Membership;
import com.mediminder.model.Role;
import com.mediminder.model.User;
import com.mediminder.repository.MembershipRepository;
import org.springframework.stereotype.Component;

@Component
public class AccessGuard {

    private final MembershipRepository membershipRepository;

    public AccessGuard(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public Membership requireMember(Long circleId, User user) {
        return membershipRepository.findByUserIdAndCareCircleId(user.getId(), circleId)
                .orElseThrow(() -> ApiException.forbidden("Du bist kein Mitglied dieses Pflegekreises."));
    }

    public Membership requireAdmin(Long circleId, User user) {
        Membership membership = requireMember(circleId, user);
        if (membership.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Diese Aktion darf nur ein Admin des Pflegekreises ausführen.");
        }
        return membership;
    }
}
