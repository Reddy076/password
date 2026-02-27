package com.revature.passwordmanager.model.files;

import com.revature.passwordmanager.model.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Feature 40 – Secure File Storage Vault.
 *
 * <p>Audit record of every file access (download) event.
 * Provides a tamper-evident history of who accessed which file and when.</p>
 */
@Entity
@Table(name = "file_access_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @lombok.ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    @lombok.ToString.Exclude
    private SecureFile file;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private FileAction action;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "accessed_at", updatable = false)
    private LocalDateTime accessedAt;

    public enum FileAction {
        UPLOAD, DOWNLOAD, DELETE, VIEW_METADATA
    }
}
