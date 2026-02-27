package com.revature.passwordmanager.model.security.breach;

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
 * Persists a confirmed breach finding for a vault entry password.
 * One row per compromised vault entry per scan.
 */
@Entity
@Table(name = "compromised_credentials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompromisedCredential {

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

    /** How many times HIBP has seen this password hash (k-anonymity count). */
    @Column(name = "pwned_count", nullable = false)
    private long pwnedCount;

    /** SHA-1 prefix used for the k-anonymity query (first 5 chars). */
    @Column(name = "hash_prefix", length = 5)
    private String hashPrefix;

    @Column(name = "is_resolved")
    @Builder.Default
    private boolean isResolved = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "detected_at", updatable = false)
    private LocalDateTime detectedAt;
}
