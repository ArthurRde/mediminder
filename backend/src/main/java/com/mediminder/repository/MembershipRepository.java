package com.mediminder.repository;

import com.mediminder.model.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    Optional<Membership> findByUserIdAndCareCircleId(Long userId, Long careCircleId);

    List<Membership> findByCareCircleIdOrderByJoinedAt(Long careCircleId);

    List<Membership> findByUserIdOrderByJoinedAt(Long userId);

    boolean existsByUserIdAndCareCircleId(Long userId, Long careCircleId);
}
