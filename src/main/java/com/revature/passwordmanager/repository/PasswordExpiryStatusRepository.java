package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.expiry.PasswordExpiryStatus;
import com.revature.passwordmanager.model.expiry.PasswordExpiryStatus.ExpiryState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Feature 38 – Password Expiration Tracker.
 */
@Repository
public interface PasswordExpiryStatusRepository extends JpaRepository<PasswordExpiryStatus, Long> {

    Optional<PasswordExpiryStatus> findByVaultEntryId(Long vaultEntryId);

    /** All expiry records for a user's vault entries. */
    @Query("SELECT s FROM PasswordExpiryStatus s WHERE s.vaultEntry.user.id = :userId AND s.vaultEntry.isDeleted = false")
    List<PasswordExpiryStatus> findAllByUserId(@Param("userId") Long userId);

    /** Entries expiring within the next N days (for the expiring-soon endpoint). */
    @Query("SELECT s FROM PasswordExpiryStatus s " +
           "WHERE s.vaultEntry.user.id = :userId " +
           "AND s.vaultEntry.isDeleted = false " +
           "AND s.expiresAt IS NOT NULL " +
           "AND s.expiresAt BETWEEN :now AND :cutoff " +
           "ORDER BY s.expiresAt ASC")
    List<PasswordExpiryStatus> findExpiringSoon(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            @Param("cutoff") LocalDateTime cutoff);

    /** All entries that have already expired (for scheduler). */
    @Query("SELECT s FROM PasswordExpiryStatus s " +
           "WHERE s.vaultEntry.isDeleted = false " +
           "AND s.expiresAt IS NOT NULL " +
           "AND s.expiresAt < :now")
    List<PasswordExpiryStatus> findAllExpired(@Param("now") LocalDateTime now);

    /** Entries in reminder window that haven't had a reminder sent yet (for scheduler). */
    @Query("SELECT s FROM PasswordExpiryStatus s " +
           "WHERE s.vaultEntry.isDeleted = false " +
           "AND s.expiresAt IS NOT NULL " +
           "AND s.expiresAt BETWEEN :now AND :reminderCutoff " +
           "AND s.reminderSent = false " +
           "AND (s.snoozedUntil IS NULL OR s.snoozedUntil < :now)")
    List<PasswordExpiryStatus> findPendingReminders(
            @Param("now") LocalDateTime now,
            @Param("reminderCutoff") LocalDateTime reminderCutoff);

    void deleteByVaultEntryId(Long vaultEntryId);

    /** All expiry records for non-deleted vault entries (used by the daily scheduler). */
    @Query("SELECT s FROM PasswordExpiryStatus s WHERE s.vaultEntry.isDeleted = false")
    List<PasswordExpiryStatus> findAllNonDeleted();
}
