package com.mediminder.dto;

import com.mediminder.model.Membership;
import com.mediminder.model.Patient;
import com.mediminder.model.Role;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public class CircleDtos {

    public record CreateCircleRequest(@NotBlank(message = "Name darf nicht leer sein") String name) {
    }

    public record PatientRequest(
            @NotBlank(message = "Name darf nicht leer sein") String name,
            @Min(value = 1900, message = "Geburtsjahr unplausibel") @Max(value = 2100, message = "Geburtsjahr unplausibel") Integer birthYear,
            String note) {
    }

    public record PatientDto(Long id, String name, Integer birthYear, String note) {
        public static PatientDto from(Patient patient) {
            return patient == null ? null
                    : new PatientDto(patient.getId(), patient.getName(), patient.getBirthYear(), patient.getNote());
        }
    }

    public record MemberDto(Long userId, String name, String email, Role role, Instant joinedAt) {
        public static MemberDto from(Membership membership) {
            return new MemberDto(
                    membership.getUser().getId(),
                    membership.getUser().getName(),
                    membership.getUser().getEmail(),
                    membership.getRole(),
                    membership.getJoinedAt());
        }
    }

    public record CircleSummaryDto(Long id, String name, Role role, String patientName) {
    }

    public record CircleDetailDto(Long id, String name, Role myRole, String inviteToken,
                                  PatientDto patient, List<MemberDto> members) {
    }

    public record InviteDto(String inviteToken, String joinPath) {
    }
}
