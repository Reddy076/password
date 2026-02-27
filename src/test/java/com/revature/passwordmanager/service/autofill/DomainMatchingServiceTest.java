package com.revature.passwordmanager.service.autofill;

import com.revature.passwordmanager.service.autofill.DomainMatchingService.MatchType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature 36 – Smart Password Autofill (Backend API).
 *
 * <p>Pure unit tests for {@link DomainMatchingService} — no Spring context needed.</p>
 */
class DomainMatchingServiceTest {

    private DomainMatchingService service;

    @BeforeEach
    void setUp() {
        service = new DomainMatchingService();
    }

    // ── extractDomain ─────────────────────────────────────────────────────────

    @Test
    void extractDomain_FullUrl_ShouldReturnDomain() {
        assertThat(service.extractDomain("https://github.com/login")).isEqualTo("github.com");
    }

    @Test
    void extractDomain_HttpUrl_ShouldReturnDomain() {
        assertThat(service.extractDomain("http://example.com/path")).isEqualTo("example.com");
    }

    @Test
    void extractDomain_WithWww_ShouldStripWww() {
        assertThat(service.extractDomain("https://www.github.com")).isEqualTo("github.com");
    }

    @Test
    void extractDomain_DomainOnly_ShouldReturnDomain() {
        assertThat(service.extractDomain("github.com")).isEqualTo("github.com");
    }

    @Test
    void extractDomain_WithSubdomain_ShouldReturnFullSubdomain() {
        assertThat(service.extractDomain("https://app.github.com/dashboard")).isEqualTo("app.github.com");
    }

    @Test
    void extractDomain_NullInput_ShouldReturnEmpty() {
        assertThat(service.extractDomain(null)).isEmpty();
    }

    @Test
    void extractDomain_BlankInput_ShouldReturnEmpty() {
        assertThat(service.extractDomain("  ")).isEmpty();
    }

    @Test
    void extractDomain_UpperCase_ShouldReturnLowerCase() {
        assertThat(service.extractDomain("HTTPS://GITHUB.COM/LOGIN")).isEqualTo("github.com");
    }

    // ── getMatchType ──────────────────────────────────────────────────────────

    @Test
    void getMatchType_ExactMatch_ShouldReturnExact() {
        MatchType result = service.getMatchType("github.com", "https://github.com/login");
        assertThat(result).isEqualTo(MatchType.EXACT);
    }

    @Test
    void getMatchType_StoredIsSubdomainOfQuery_ShouldReturnSubdomain() {
        // "app.github.com" is a subdomain of "github.com"
        MatchType result = service.getMatchType("github.com", "https://app.github.com");
        assertThat(result).isEqualTo(MatchType.SUBDOMAIN);
    }

    @Test
    void getMatchType_QueryIsSubdomainOfStored_ShouldReturnSubdomain() {
        // "github.com" matches "app.github.com" (stored is more specific)
        MatchType result = service.getMatchType("app.github.com", "https://github.com");
        assertThat(result).isEqualTo(MatchType.SUBDOMAIN);
    }

    @Test
    void getMatchType_SameBaseDomain_ShouldReturnPartial() {
        // "github.com" and "github.io" share base "github"
        MatchType result = service.getMatchType("github.com", "https://github.io");
        assertThat(result).isEqualTo(MatchType.PARTIAL);
    }

    @Test
    void getMatchType_NoMatch_ShouldReturnNoMatch() {
        MatchType result = service.getMatchType("github.com", "https://google.com");
        assertThat(result).isEqualTo(MatchType.NO_MATCH);
    }

    @Test
    void getMatchType_NullQueryDomain_ShouldReturnNoMatch() {
        MatchType result = service.getMatchType(null, "https://github.com");
        assertThat(result).isEqualTo(MatchType.NO_MATCH);
    }

    @Test
    void getMatchType_NullStoredUrl_ShouldReturnNoMatch() {
        MatchType result = service.getMatchType("github.com", null);
        assertThat(result).isEqualTo(MatchType.NO_MATCH);
    }

    @Test
    void getMatchType_WithWwwInStored_ShouldStillMatch() {
        MatchType result = service.getMatchType("github.com", "https://www.github.com");
        assertThat(result).isEqualTo(MatchType.EXACT);
    }

    @Test
    void getMatchType_BankExample_ShouldMatchExact() {
        MatchType result = service.getMatchType("chase.com", "https://chase.com/login");
        assertThat(result).isEqualTo(MatchType.EXACT);
    }

    // ── matchScore ────────────────────────────────────────────────────────────

    @Test
    void matchScore_Exact_ShouldBeHighest() {
        assertThat(service.matchScore(MatchType.EXACT)).isGreaterThan(service.matchScore(MatchType.SUBDOMAIN));
    }

    @Test
    void matchScore_Subdomain_ShouldBeHigherThanPartial() {
        assertThat(service.matchScore(MatchType.SUBDOMAIN)).isGreaterThan(service.matchScore(MatchType.PARTIAL));
    }

    @Test
    void matchScore_NoMatch_ShouldBeZero() {
        assertThat(service.matchScore(MatchType.NO_MATCH)).isEqualTo(0);
    }

    // ── getBaseDomain ─────────────────────────────────────────────────────────

    @Test
    void getBaseDomain_TwoParts_ShouldReturnFirstPart() {
        assertThat(service.getBaseDomain("github.com")).isEqualTo("github");
    }

    @Test
    void getBaseDomain_ThreeParts_ShouldReturnFirstPart() {
        assertThat(service.getBaseDomain("app.github.com")).isEqualTo("app");
    }

    @Test
    void getBaseDomain_NoDot_ShouldReturnWholeDomain() {
        assertThat(service.getBaseDomain("localhost")).isEqualTo("localhost");
    }
}
