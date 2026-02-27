package com.revature.passwordmanager.service.autofill;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;

/**
 * Feature 36 – Smart Password Autofill (Backend API).
 *
 * <p>Pure computation component — no database access.
 * Extracts domains from URLs and determines the match type between
 * a query domain and a stored vault entry URL.</p>
 *
 * <p>Match types (in order of relevance):</p>
 * <ul>
 *   <li><strong>EXACT</strong> — domains are identical (e.g., "github.com" == "github.com")</li>
 *   <li><strong>SUBDOMAIN</strong> — stored URL is a subdomain of the query (e.g., "app.github.com" matches "github.com")</li>
 *   <li><strong>PARTIAL</strong> — the base domain (TLD-stripped) matches (e.g., "github" in "github.io" matches "github.com")</li>
 *   <li><strong>NO_MATCH</strong> — no match</li>
 * </ul>
 */
@Component
public class DomainMatchingService {

    public enum MatchType {
        EXACT, SUBDOMAIN, PARTIAL, NO_MATCH
    }

    /**
     * Extracts the hostname from a URL string.
     * Handles URLs with and without schemes.
     *
     * @param url the URL to extract from (e.g., "https://github.com/login")
     * @return the lowercase hostname (e.g., "github.com"), or the input if parsing fails
     */
    public String extractDomain(String url) {
        if (url == null || url.isBlank()) return "";

        String normalized = url.trim().toLowerCase(Locale.ROOT);

        // Add scheme if missing so URI can parse it
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }

        try {
            URI uri = URI.create(normalized);
            String host = uri.getHost();
            if (host == null) return normalized;
            // Strip "www." prefix
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            // Fallback: strip scheme and path manually
            String stripped = normalized.replaceFirst("https?://", "");
            int slashIdx = stripped.indexOf('/');
            String host = slashIdx >= 0 ? stripped.substring(0, slashIdx) : stripped;
            return host.startsWith("www.") ? host.substring(4) : host;
        }
    }

    /**
     * Determines the match type between a query domain and a stored vault entry URL.
     *
     * @param queryDomain  the domain extracted from the current page URL (e.g., "github.com")
     * @param storedUrl    the websiteUrl stored in the vault entry (e.g., "https://github.com")
     * @return the match type
     */
    public MatchType getMatchType(String queryDomain, String storedUrl) {
        if (queryDomain == null || queryDomain.isBlank() || storedUrl == null || storedUrl.isBlank()) {
            return MatchType.NO_MATCH;
        }

        String storedDomain = extractDomain(storedUrl);
        String query = queryDomain.toLowerCase(Locale.ROOT);
        String stored = storedDomain.toLowerCase(Locale.ROOT);

        // EXACT match
        if (query.equals(stored)) {
            return MatchType.EXACT;
        }

        // SUBDOMAIN match: stored is a subdomain of query (e.g., "app.github.com" matches "github.com")
        if (stored.endsWith("." + query)) {
            return MatchType.SUBDOMAIN;
        }

        // SUBDOMAIN match: query is a subdomain of stored (e.g., "github.com" matches "app.github.com")
        if (query.endsWith("." + stored)) {
            return MatchType.SUBDOMAIN;
        }

        // PARTIAL match: base domain (before first dot) matches
        String queryBase = getBaseDomain(query);
        String storedBase = getBaseDomain(stored);
        if (!queryBase.isEmpty() && queryBase.equals(storedBase)) {
            return MatchType.PARTIAL;
        }

        return MatchType.NO_MATCH;
    }

    /**
     * Returns the base domain (the part before the first dot).
     * E.g., "github.com" → "github", "app.github.io" → "app"
     * For single-label domains (no dot), returns the domain itself.
     */
    public String getBaseDomain(String domain) {
        if (domain == null || domain.isBlank()) return "";
        int dotIdx = domain.indexOf('.');
        return dotIdx >= 0 ? domain.substring(0, dotIdx) : domain;
    }

    /**
     * Returns the second-level domain (SLD) — the part just before the TLD.
     * E.g., "app.github.com" → "github", "github.com" → "github"
     */
    public String getSecondLevelDomain(String domain) {
        if (domain == null || domain.isBlank()) return "";
        String[] parts = domain.split("\\.");
        if (parts.length >= 2) {
            return parts[parts.length - 2];
        }
        return domain;
    }

    /**
     * Returns the match score for sorting (higher = better match).
     */
    public int matchScore(MatchType type) {
        return switch (type) {
            case EXACT -> 3;
            case SUBDOMAIN -> 2;
            case PARTIAL -> 1;
            case NO_MATCH -> 0;
        };
    }
}
