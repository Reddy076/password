package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.expiry.ExpiryReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Feature 38 – Password Expiration Tracker.
 */
@Repository
public interface ExpiryReminderRepository extends JpaRepository<ExpiryReminder, Long> {

    List<ExpiryReminder> findByUserIdOrderBySentAtDesc(Long userId);

    List<ExpiryReminder> findByVaultEntryIdOrderBySentAtDesc(Long vaultEntryId);

    void deleteByVaultEntryId(Long vaultEntryId);
}
