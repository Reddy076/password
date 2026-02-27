package com.revature.passwordmanager.model.expiry;

import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Feature 38 – Password Expiration Tracker.
 *
 * <p>Audit record of every reminder notification sent for a vault entry expiry.
 * Allows the scheduler to avoid sending duplicate reminders and provides a
 * history of when reminders were dispatched.</p>
 */
@Entity
@Table(name = "expiry_reminders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @lombok.ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vault_entry_id", nullable = false)
    @lombok.ToString.Exclude
    private VaultEntry vaultEntry;

    /** The expiry state that triggered this reminder. */
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_state", nullable = false, length = 20)
    private PasswordExpiryStatus.ExpiryState triggerState;

    /** When the reminder was sent. */
    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;

    /** Whether the user snoozed this reminder. */
    @Column(name = "snoozed")
    @Builder.Default
    private Boolean snoozed = false;
}
