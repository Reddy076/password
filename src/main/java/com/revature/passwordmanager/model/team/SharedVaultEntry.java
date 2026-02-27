package com.revature.passwordmanager.model.team;

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
 * Feature 42 – Team/Family Vault Sharing.
 *
 * <p>Links a vault entry to a team, making it visible to all team members
 * according to their role. The entry owner retains full control and can
 * unshare at any time.</p>
 */
@Entity
@Table(name = "shared_vault_entries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "vault_entry_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedVaultEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    @lombok.ToString.Exclude
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vault_entry_id", nullable = false)
    @lombok.ToString.Exclude
    private VaultEntry vaultEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_by", nullable = false)
    @lombok.ToString.Exclude
    private User sharedBy;

    @CreationTimestamp
    @Column(name = "shared_at", updatable = false)
    private LocalDateTime sharedAt;
}
