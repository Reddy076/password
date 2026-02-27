package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.emergency.EmergencyAccessRequest;
import com.revature.passwordmanager.model.emergency.EmergencyAccessRequest.AccessStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 */
@Repository
public interface EmergencyAccessRequestRepository extends JpaRepository<EmergencyAccessRequest, Long> {

    /** All requests for a vault owner (for the owner to see who has requested access). */
    List<EmergencyAccessRequest> findByUserIdOrderByRequestedAtDesc(Long userId);

    /** All pending requests for a vault owner. */
    List<EmergencyAccessRequest> findByUserIdAndStatus(Long userId, AccessStatus status);

    /** Find a request by its access token (used when the contact accesses the vault). */
    Optional<EmergencyAccessRequest> findByAccessToken(String accessToken);

    /** Find by id and owner (for ownership checks when owner denies/grants). */
    Optional<EmergencyAccessRequest> findByIdAndUserId(Long id, Long userId);

    /** Find by id and contact (for contact to view their own requests). */
    Optional<EmergencyAccessRequest> findByIdAndContactId(Long id, Long contactId);

    /** All requests made by a specific contact. */
    List<EmergencyAccessRequest> findByContactIdOrderByRequestedAtDesc(Long contactId);

    /**
     * All PENDING requests whose waiting period has elapsed — used by the scheduler
     * to auto-approve them.
     */
    @Query("SELECT r FROM EmergencyAccessRequest r " +
           "WHERE r.status = 'PENDING' " +
           "AND r.waitingPeriodEndsAt IS NOT NULL " +
           "AND r.waitingPeriodEndsAt <= :now")
    List<EmergencyAccessRequest> findPendingRequestsReadyForApproval(@Param("now") LocalDateTime now);

    /**
     * All APPROVED requests whose access token has expired — used by the scheduler
     * to mark them EXPIRED.
     */
    @Query("SELECT r FROM EmergencyAccessRequest r " +
           "WHERE r.status = 'APPROVED' " +
           "AND r.expiresAt IS NOT NULL " +
           "AND r.expiresAt <= :now")
    List<EmergencyAccessRequest> findApprovedRequestsReadyForExpiry(@Param("now") LocalDateTime now);

    /** Check if there is already a PENDING request from this contact for this user. */
    boolean existsByContactIdAndUserIdAndStatus(Long contactId, Long userId, AccessStatus status);
}
