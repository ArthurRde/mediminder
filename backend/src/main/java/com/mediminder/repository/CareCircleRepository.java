package com.mediminder.repository;

import com.mediminder.model.CareCircle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CareCircleRepository extends JpaRepository<CareCircle, Long> {

    Optional<CareCircle> findByInviteToken(String inviteToken);
}
