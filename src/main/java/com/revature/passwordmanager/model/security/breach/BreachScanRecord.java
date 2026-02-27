package com.revature.passwordmanager.model.security.breach;

import com.revature.passwordmanager.model.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Audit record of each breach scan run (manual or scheduled).
 */
@Entity
@Table(name = "breach_scan_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreachScanRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @lombok.ToString.Exclude
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private TriggerType triggerType;

    @Column(name = "entries_scanned", nullable = false)
    private int entriesScanned;

    @Column(name = "compromised_found", nullable = false)
    private int compromisedFound;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ScanStatus status = ScanStatus.COMPLETED;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "scanned_at", updatable = false)
    private LocalDateTime scannedAt;

    public enum TriggerType {
        MANUAL, SCHEDULED
    }

    public enum ScanStatus {
        IN_PROGRESS, COMPLETED, FAILED
    }
}
