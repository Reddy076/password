package com.revature.passwordmanager.service.autofill;

import com.revature.passwordmanager.dto.request.AutofillSuggestionRequest;
import com.revature.passwordmanager.dto.response.AutofillSuggestionResponse;
import com.revature.passwordmanager.dto.response.AutofillSuggestionResponse.AutofillEntry;
import com.revature.passwordmanager.model.autofill.AutofillUsageLog;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.AutofillUsageLogRepository;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.repository.VaultEntryRepository;
import com.revature.passwordmanager.service.autofill.DomainMatchingService.MatchType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Feature 36 – Smart Password Autofill (Backend API).
 *
 * <p>Orchestrates domain-based vault entry lookup for the autofill feature.
 * Passwords are NEVER returned — only metadata (id, title, username, websiteUrl).</p>
 */
@Service
@RequiredArgsConstructor
public class AutofillService {

    private final UserRepository userRepository;
    private final VaultEntryRepository vaultEntryRepository;
    private final AutofillUsageLogRepository usageLogRepository;
    private final DomainMatchingService domainMatchingService;

    // ── Suggestions ───────────────────────────────────────────────────────────

    /**
     * Returns vault entries that match the domain of the given URL.
     * Entries are sorted by match quality: EXACT > SUBDOMAIN > PARTIAL.
     * Highly sensitive entries are excluded from suggestions.
     *
     * @param username the authenticated user
     * @param request  the autofill suggestion request (contains the page URL)
     * @return matching vault entries (no passwords)
     */
    @Transactional(readOnly = true)
    public AutofillSuggestionResponse getSuggestions(String username, AutofillSuggestionRequest request) {
        User user = userRepository.findByUsernameOrThrow(username);
        String queryDomain = domainMatchingService.extractDomain(request.getUrl());

        List<VaultEntry> allEntries = vaultEntryRepository.findByUserIdAndIsDeletedFalse(user.getId());

        List<ScoredEntry> scored = new ArrayList<>();
        for (VaultEntry entry : allEntries) {
            // Skip highly sensitive entries
            if (Boolean.TRUE.equals(entry.getIsHighlySensitive())) continue;
            // Skip entries without a website URL
            if (entry.getWebsiteUrl() == null || entry.getWebsiteUrl().isBlank()) continue;

            MatchType matchType = domainMatchingService.getMatchType(queryDomain, entry.getWebsiteUrl());
            if (matchType != MatchType.NO_MATCH) {
                scored.add(new ScoredEntry(entry, matchType));
            }
        }

        // Sort by match score descending, then by title ascending
        scored.sort(Comparator
                .comparingInt((ScoredEntry s) -> domainMatchingService.matchScore(s.matchType()))
                .reversed()
                .thenComparing(s -> s.entry().getTitle()));

        List<AutofillEntry> suggestions = scored.stream()
                .map(s -> AutofillEntry.builder()
                        .entryId(s.entry().getId())
                        .title(s.entry().getTitle())
                        .username(s.entry().getUsername()) // encrypted — extension shows as hint
                        .websiteUrl(s.entry().getWebsiteUrl())
                        .matchType(s.matchType().name())
                        .isFavorite(s.entry().getIsFavorite())
                        .build())
                .toList();

        return AutofillSuggestionResponse.builder()
                .domain(queryDomain)
                .suggestions(suggestions)
                .totalCount(suggestions.size())
                .build();
    }

    // ── Trusted domains ───────────────────────────────────────────────────────

    /**
     * Returns the list of domains the user has previously used autofill on,
     * ordered by most recently used.
     */
    @Transactional(readOnly = true)
    public List<String> getTrustedDomains(String username) {
        User user = userRepository.findByUsernameOrThrow(username);
        return usageLogRepository.findDistinctDomainsByUserId(user.getId());
    }

    // ── Log usage ─────────────────────────────────────────────────────────────

    /**
     * Records that autofill was used (or suggested) for a domain.
     *
     * @param username     the authenticated user
     * @param url          the page URL where autofill was used
     * @param vaultEntryId the vault entry id that was selected (null if no entry was selected)
     * @param applied      whether the autofill was actually applied (true) or just suggested (false)
     */
    @Transactional
    public void logUsage(String username, String url, Long vaultEntryId, boolean applied) {
        User user = userRepository.findByUsernameOrThrow(username);
        String domain = domainMatchingService.extractDomain(url);

        AutofillUsageLog log = AutofillUsageLog.builder()
                .user(user)
                .domain(domain)
                .vaultEntryId(vaultEntryId)
                .applied(applied)
                .build();
        usageLogRepository.save(log);
    }

    // ── Internal record ───────────────────────────────────────────────────────

    private record ScoredEntry(VaultEntry entry, MatchType matchType) {}
}
