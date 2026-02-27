package com.revature.passwordmanager.model.security.breach;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BreachCheckResult} covering the factory methods and field contracts.
 *
 * <p>These tests close the gap identified in the Feature 34 analysis:
 * {@code BreachCheckResult} was listed in the proposal but was missing from the codebase,
 * meaning scan results were represented as raw primitive longs instead of structured objects.</p>
 */
class BreachCheckResultTest {

    // ── Factory: clean ────────────────────────────────────────────────────────

    @Test
    void clean_ShouldReturnNotCompromisedResult() {
        BreachCheckResult result = BreachCheckResult.clean(10L, "Netflix", "ABC12");

        assertThat(result.getVaultEntryId()).isEqualTo(10L);
        assertThat(result.getEntryTitle()).isEqualTo("Netflix");
        assertThat(result.isCompromised()).isFalse();
        assertThat(result.getPwnedCount()).isZero();
        assertThat(result.getHashPrefix()).isEqualTo("ABC12");
        assertThat(result.isCheckSucceeded()).isTrue();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void clean_ShouldIndicateSuccessfulCheck() {
        BreachCheckResult result = BreachCheckResult.clean(1L, "Gmail", "XYZ99");

        assertThat(result.isCheckSucceeded()).isTrue();
        assertThat(result.isCompromised()).isFalse();
    }

    // ── Factory: compromised ──────────────────────────────────────────────────

    @Test
    void compromised_ShouldReturnCompromisedResult() {
        BreachCheckResult result = BreachCheckResult.compromised(20L, "Bank Login", "DEF45", 12345L);

        assertThat(result.getVaultEntryId()).isEqualTo(20L);
        assertThat(result.getEntryTitle()).isEqualTo("Bank Login");
        assertThat(result.isCompromised()).isTrue();
        assertThat(result.getPwnedCount()).isEqualTo(12345L);
        assertThat(result.getHashPrefix()).isEqualTo("DEF45");
        assertThat(result.isCheckSucceeded()).isTrue();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void compromised_SingleOccurrence_ShouldReturnCount1() {
        BreachCheckResult result = BreachCheckResult.compromised(5L, "Twitter", "GHI78", 1L);

        assertThat(result.isCompromised()).isTrue();
        assertThat(result.getPwnedCount()).isEqualTo(1L);
        assertThat(result.isCheckSucceeded()).isTrue();
    }

    @Test
    void compromised_HighPwnedCount_ShouldPreserveCount() {
        BreachCheckResult result = BreachCheckResult.compromised(5L, "Rockyou", "A1B2C", 10_000_000L);

        assertThat(result.getPwnedCount()).isEqualTo(10_000_000L);
        assertThat(result.isCompromised()).isTrue();
    }

    // ── Factory: failed ───────────────────────────────────────────────────────

    @Test
    void failed_ShouldReturnFailedCheck() {
        BreachCheckResult result = BreachCheckResult.failed(30L, "Amazon", "HIBP API unavailable");

        assertThat(result.getVaultEntryId()).isEqualTo(30L);
        assertThat(result.getEntryTitle()).isEqualTo("Amazon");
        assertThat(result.isCompromised()).isFalse();
        assertThat(result.getPwnedCount()).isZero();
        assertThat(result.isCheckSucceeded()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("HIBP API unavailable");
    }

    @Test
    void failed_ShouldNotIndicateCompromised() {
        BreachCheckResult result = BreachCheckResult.failed(1L, "Test", "timeout");

        // A failed check must NOT be treated as "safe" (isCheckSucceeded==false signals inconclusive)
        assertThat(result.isCheckSucceeded()).isFalse();
        assertThat(result.isCompromised()).isFalse();
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    @Test
    void builder_AllFieldsExplicit_ShouldPopulateCorrectly() {
        BreachCheckResult result = BreachCheckResult.builder()
                .vaultEntryId(99L)
                .entryTitle("Custom Entry")
                .compromised(true)
                .pwnedCount(500L)
                .hashPrefix("ZZZZZ")
                .checkSucceeded(true)
                .errorMessage(null)
                .build();

        assertThat(result.getVaultEntryId()).isEqualTo(99L);
        assertThat(result.getEntryTitle()).isEqualTo("Custom Entry");
        assertThat(result.isCompromised()).isTrue();
        assertThat(result.getPwnedCount()).isEqualTo(500L);
        assertThat(result.getHashPrefix()).isEqualTo("ZZZZZ");
        assertThat(result.isCheckSucceeded()).isTrue();
        assertThat(result.getErrorMessage()).isNull();
    }

    // ── Equality / Lombok ─────────────────────────────────────────────────────

    @Test
    void twoCleanResultsForSameEntry_ShouldBeEqual() {
        BreachCheckResult r1 = BreachCheckResult.clean(1L, "Gmail", "PREFIX");
        BreachCheckResult r2 = BreachCheckResult.clean(1L, "Gmail", "PREFIX");

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    void compromisedAndCleanResult_ForSameEntry_ShouldNotBeEqual() {
        BreachCheckResult clean = BreachCheckResult.clean(1L, "Gmail", "PREFIX");
        BreachCheckResult comp = BreachCheckResult.compromised(1L, "Gmail", "PREFIX", 100L);

        assertThat(clean).isNotEqualTo(comp);
    }
}
