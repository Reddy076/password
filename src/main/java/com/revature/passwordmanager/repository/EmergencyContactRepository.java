package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.emergency.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 */
@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {

    /** All active contacts for a vault owner. */
    List<EmergencyContact> findByUserIdAndActiveTrue(Long userId);

    /** Find a specific active contact by owner and contact email. */
    Optional<EmergencyContact> findByUserIdAndContactEmailAndActiveTrue(Long userId, String contactEmail);

    /** Find by id and owner (for ownership checks). */
    Optional<EmergencyContact> findByIdAndUserId(Long id, Long userId);

    /** Find contacts by contact email (used when the contact requests access). */
    List<EmergencyContact> findByContactEmailAndActiveTrue(String contactEmail);
}
