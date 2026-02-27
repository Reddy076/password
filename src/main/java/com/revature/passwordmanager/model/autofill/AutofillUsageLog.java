package com.revature.passwordmanager.model.autofill;

import com.revature.passwordmanager.model.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Feature 36 – Smart Password Autofill (Backend API).
 *
 * <p>Records each time the autofill API was used for a specific domain.
 * Used to build the "trusted domains" list and usage analytics.</p>
 */
@Entity
@Table(name = "autofill_usage_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutofillUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @lombok.ToString.Exclude
    private User user;

    /** The domain that was autofilled (e.g., "github.com"). */
    @Column(name = "domain", nullable = false, length = 255)
    private String domain;

    /** The vault entry id that was used for autofill (null if no match was selected). */
    @Column(name = "vault_entry_id")
    private Long vaultEntryId;

    /** Whether the autofill was actually applied (true) or just suggested (false). */
    @Column(name = "applied", nullable = false)
    @Builder.Default
    private Boolean applied = false;

    @CreationTimestamp
    @Column(name = "used_at", updatable = false)
    private LocalDateTime usedAt;
}
