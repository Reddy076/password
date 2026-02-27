package com.revature.passwordmanager.model.security.breach;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transient (non-persisted) result of a single password check against the
 * HaveIBeenPwned k-anonymity API for one vault entry.
 *
 * <p>This class is produced by {@link com.revature.passwordmanager.service.security.breach.HaveIBeenPwnedClient}
 * for each vault entry during a scan and consumed by
 * {@link com.revature.passwordmanager.service.security.breach.BreachMonitorService}
 * to decide whether to persist a {@link CompromisedCredential}.</p>
 *
 * <p>It is intentionally not a JPA entity — it is an in-memory intermediate DTO
 * that represents the outcome of a single HIBP range API call. The gap analysis
 * identified that this class was proposed in the feature spec but was missing;
 * its absence caused the scan loop to use primitive boolean returns instead of
 * structured, easily testable result objects.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreachCheckResult {

    /**
     * The vault entry id that was checked.
     */
    private Long vaultEntryId;

    /**
     * The title of the vault entry (for display/notification purposes).
     */
    private String entryTitle;

    /**
     * Whether this password was found in any known data breach.
     */
    private boolean compromised;

    /**
     * The number of times this password hash appeared in HIBP breach data.
     * Zero if {@code compromised == false}.
     */
    private long pwnedCount;

    /**
     * The first 5 characters of the SHA-1 hash sent to HIBP (the k-anonymity prefix).
     * Stored for audit/debugging; never the full hash.
     */
    private String hashPrefix;

    /**
     * True if the HIBP API call succeeded; false if there was a network or parse error.
     * A failed check should be treated as inconclusive, not as "safe".
     */
    private boolean checkSucceeded;

    /**
     * Error message populated only when {@code checkSucceeded == false}.
     * Null on successful checks.
     */
    private String errorMessage;

    // ── Factory helpers ───────────────────────────────────────────────────────

    /**
     * Creates a result indicating a clean (not compromised) check.
     */
    public static BreachCheckResult clean(Long vaultEntryId, String entryTitle, String hashPrefix) {
        return BreachCheckResult.builder()
                .vaultEntryId(vaultEntryId)
                .entryTitle(entryTitle)
                .compromised(false)
                .pwnedCount(0)
                .hashPrefix(hashPrefix)
                .checkSucceeded(true)
                .build();
    }

    /**
     * Creates a result indicating a compromised password found in HIBP.
     */
    public static BreachCheckResult compromised(Long vaultEntryId, String entryTitle,
                                                 String hashPrefix, long pwnedCount) {
        return BreachCheckResult.builder()
                .vaultEntryId(vaultEntryId)
                .entryTitle(entryTitle)
                .compromised(true)
                .pwnedCount(pwnedCount)
                .hashPrefix(hashPrefix)
                .checkSucceeded(true)
                .build();
    }

    /**
     * Creates a result indicating the check could not be completed due to an error.
     */
    public static BreachCheckResult failed(Long vaultEntryId, String entryTitle, String errorMessage) {
        return BreachCheckResult.builder()
                .vaultEntryId(vaultEntryId)
                .entryTitle(entryTitle)
                .compromised(false)
                .pwnedCount(0)
                .checkSucceeded(false)
                .errorMessage(errorMessage)
                .build();
    }
}
