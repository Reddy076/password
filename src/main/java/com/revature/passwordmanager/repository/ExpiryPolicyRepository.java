package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.expiry.ExpiryPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Feature 38 – Password Expiration Tracker.
 */
@Repository
public interface ExpiryPolicyRepository extends JpaRepository<ExpiryPolicy, Long> {

    Optional<ExpiryPolicy> findByUserId(Long userId);
}
