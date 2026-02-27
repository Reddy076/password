package com.revature.passwordmanager.service.analytics;

import com.revature.passwordmanager.dto.response.TimelineEventDTO;
import com.revature.passwordmanager.model.security.AuditLog;
import com.revature.passwordmanager.model.security.AuditLog.AuditAction;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.VaultEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Transforms raw {@link AuditLog} records into enriched {@link TimelineEventDTO} objects.
 * <p>
 * Entry titles are extracted from the audit log {@code details} field using the
 * conventions set by VaultService and SecureShareService, then cross-referenced
 * against the vault entry table to resolve vault entry IDs and website URLs.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class ActivityAggregator {

    private final VaultEntryRepository vaultEntryRepository;

    /**
     * Converts a list of audit log entries into timeline event DTOs.
     *
     * @param logs   raw audit log entries for a user
     * @param userId the owning user's id, used to look up vault entries by title
     * @return ordered list of TimelineEventDTOs (preserving input order)
     */
    public List<TimelineEventDTO> aggregate(List<AuditLog> logs, Long userId) {
        // Pre-fetch all vault entries for this user (including deleted) to resolve IDs/websiteUrl by title
        Map<String, VaultEntry> titleCache = buildTitleCache(userId);

        List<TimelineEventDTO> result = new ArrayList<>();
        for (AuditLog log : logs) {
            TimelineEventDTO dto = mapLogToEvent(log, titleCache);
            result.add(dto);
        }
        return result;
    }

    /**
     * Converts a single audit log entry to a timeline event DTO.
     *
     * @param log        the audit log record
     * @param titleCache a map of (lowercase title) -> VaultEntry for ID/URL resolution
     * @return the enriched TimelineEventDTO
     */
    public TimelineEventDTO mapLogToEvent(AuditLog log, Map<String, VaultEntry> titleCache) {
        AuditAction action = log.getAction();

        String category = resolveCategory(action);
        String severity = resolveSeverity(action);
        String description = buildDescription(log, action);

        // Try to extract the entry title from the details string, then look up entry metadata
        String extractedTitle = extractEntryTitle(log.getDetails(), action);
        Long vaultEntryId = null;
        String vaultEntryTitle = null;
        String websiteUrl = null;

        if (extractedTitle != null) {
            vaultEntryTitle = extractedTitle;
            // Resolve id + websiteUrl from cache
            VaultEntry entry = titleCache.get(extractedTitle.toLowerCase());
            if (entry != null) {
                vaultEntryId = entry.getId();
                websiteUrl = entry.getWebsiteUrl();
            }
        }

        return TimelineEventDTO.builder()
                .id(log.getId())
                .eventType(action.name())
                .category(category)
                .description(description)
                .vaultEntryId(vaultEntryId)
                .vaultEntryTitle(vaultEntryTitle)
                .websiteUrl(websiteUrl)
                .ipAddress(log.getIpAddress())
                .timestamp(log.getTimestamp())
                .severity(severity)
                .build();
    }

    // ── Category resolution ───────────────────────────────────────────────────

    /**
     * Maps an AuditAction to a high-level event category for frontend color-coding.
     */
    public String resolveCategory(AuditAction action) {
        return switch (action) {
            case ENTRY_CREATED, ENTRY_UPDATED, ENTRY_DELETED, ENTRY_RESTORED,
                 PASSWORD_VIEWED -> "VAULT";
            case LOGIN, LOGIN_FAILED, LOGOUT -> "AUTH";
            case BREACH_SCAN_RUN, BREACH_DETECTED, BREACH_RESOLVED -> "BREACH";
            case SHARE_CREATED, SHARE_ACCESSED, SHARE_REVOKED -> "SHARING";
            case VAULT_EXPORTED -> "BACKUP";
            case DASHBOARD_VIEWED, TIMELINE_VIEWED -> "SECURITY";
            default -> "SECURITY";
        };
    }

    /**
     * Maps an AuditAction to a severity level for frontend highlighting.
     */
    public String resolveSeverity(AuditAction action) {
        return switch (action) {
            case BREACH_DETECTED, LOGIN_FAILED -> "CRITICAL";
            case ENTRY_DELETED, SHARE_CREATED, BREACH_SCAN_RUN -> "HIGH";
            case ENTRY_UPDATED, ENTRY_RESTORED, SHARE_ACCESSED, SHARE_REVOKED,
                 VAULT_EXPORTED, BREACH_RESOLVED -> "MEDIUM";
            case ENTRY_CREATED, PASSWORD_VIEWED, LOGIN, LOGOUT,
                 DASHBOARD_VIEWED, TIMELINE_VIEWED -> "LOW";
            default -> "LOW";
        };
    }

    // ── Description builder ───────────────────────────────────────────────────

    private String buildDescription(AuditLog log, AuditAction action) {
        String details = log.getDetails();
        // For most events the audit log already stores a good human-readable detail string.
        // We return it directly for description, enriching with IP context where relevant.
        return switch (action) {
            case LOGIN -> "Successful login" + (log.getIpAddress() != null ? " from " + log.getIpAddress() : "");
            case LOGIN_FAILED -> "Failed login attempt" + (log.getIpAddress() != null ? " from " + log.getIpAddress() : "");
            case LOGOUT -> "Session ended";
            case BREACH_SCAN_RUN -> "Breach scan executed against HaveIBeenPwned";
            case DASHBOARD_VIEWED -> "Security dashboard accessed";
            case TIMELINE_VIEWED -> "Vault timeline accessed";
            default -> details != null ? details : action.name().replace('_', ' ').toLowerCase();
        };
    }

    // ── Entry title extraction ────────────────────────────────────────────────

    /**
     * Extracts the vault entry title from an audit log details string.
     * <p>
     * VaultService uses these detail formats:
     * <ul>
     *   <li>"Created entry: &lt;title&gt;"</li>
     *   <li>"Updated entry: &lt;title&gt;"</li>
     *   <li>"Deleted entry: &lt;title&gt;"</li>
     *   <li>"Viewed password for entry: &lt;title&gt;"</li>
     *   <li>"Toggled sensitive flag for entry: &lt;title&gt;"</li>
     *   <li>"Moved to trash: &lt;title&gt;"</li>
     * </ul>
     * SecureShareService uses:
     * <ul>
     *   <li>"Shared entry '&lt;title&gt;' token=..."</li>
     *   <li>"Share revoked: id=N entry='&lt;title&gt;'"</li>
     *   <li>"Share accessed: token=..."  (no entry title)</li>
     * </ul>
     * AuditLogAspect (trash restore) uses:
     * <ul>
     *   <li>"Restored entry: &lt;title&gt; ..."</li>
     * </ul>
     * </p>
     *
     * @param details the audit log details string
     * @param action  the audit action, used to select the right parser
     * @return extracted entry title, or {@code null} if no title could be parsed
     */
    public String extractEntryTitle(String details, AuditAction action) {
        if (details == null || details.isBlank()) return null;

        return switch (action) {
            case ENTRY_CREATED -> extractAfterPrefix(details, "Created entry: ");
            case ENTRY_UPDATED -> {
                String t = extractAfterPrefix(details, "Updated entry: ");
                if (t == null) t = extractAfterPrefix(details, "Toggled sensitive flag for entry: ");
                yield t;
            }
            case ENTRY_DELETED -> extractAfterPrefix(details, "Deleted entry: ");
            case ENTRY_RESTORED -> extractAfterPrefix(details, "Restored entry: ");
            case PASSWORD_VIEWED -> extractAfterPrefix(details, "Viewed password for entry: ");
            case SHARE_CREATED -> extractQuotedTitle(details, "Shared entry '");
            case SHARE_REVOKED -> extractQuotedTitle(details, "entry='");
            default -> null;
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Extracts everything after a given prefix, stopping at the first space-separated
     * metadata separator (common stop chars: newline, " token=", " id=").
     */
    private String extractAfterPrefix(String details, String prefix) {
        if (!details.startsWith(prefix)) return null;
        String rest = details.substring(prefix.length()).trim();
        // Trim at common metadata delimiters
        for (String stop : new String[]{"' token=", " token=", " (", "\n"}) {
            int idx = rest.indexOf(stop);
            if (idx > 0) rest = rest.substring(0, idx);
        }
        return rest.isBlank() ? null : rest.trim();
    }

    /**
     * Extracts a title enclosed in single quotes starting after the given prefix.
     * E.g. for {@code "Shared entry 'Netflix' token=abc"} with prefix {@code "Shared entry '"}:
     * returns {@code "Netflix"}.
     */
    private String extractQuotedTitle(String details, String prefix) {
        int start = details.indexOf(prefix);
        if (start < 0) return null;
        int titleStart = start + prefix.length();
        int titleEnd = details.indexOf("'", titleStart);
        if (titleEnd < 0) return null;
        String title = details.substring(titleStart, titleEnd).trim();
        return title.isBlank() ? null : title;
    }

    /**
     * Builds a map of (lowercase title) -> VaultEntry for a user.
     * Includes deleted entries so historical events can still be linked.
     * In case of title collision (two entries with the same title), the most
     * recently created non-deleted entry takes precedence; if both are deleted,
     * either will do.
     */
    Map<String, VaultEntry> buildTitleCache(Long userId) {
        List<VaultEntry> all = vaultEntryRepository.findByUserId(userId);
        Map<String, VaultEntry> cache = new HashMap<>();
        for (VaultEntry e : all) {
            String key = e.getTitle().toLowerCase();
            VaultEntry existing = cache.get(key);
            // Prefer active entries over deleted ones; otherwise keep the one we have
            if (existing == null ||
                    (Boolean.TRUE.equals(existing.getIsDeleted()) && !Boolean.TRUE.equals(e.getIsDeleted()))) {
                cache.put(key, e);
            }
        }
        return cache;
    }
}
