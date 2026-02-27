package com.revature.passwordmanager.service.autofill;

import com.revature.passwordmanager.dto.request.AutofillSuggestionRequest;
import com.revature.passwordmanager.dto.response.AutofillSuggestionResponse;
import com.revature.passwordmanager.model.autofill.AutofillUsageLog;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.AutofillUsageLogRepository;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.repository.VaultEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Feature 36 – Smart Password Autofill (Backend API).
 */
@ExtendWith(MockitoExtension.class)
class AutofillServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private VaultEntryRepository vaultEntryRepository;
    @Mock private AutofillUsageLogRepository usageLogRepository;
    @Mock private DomainMatchingService domainMatchingService;

    @InjectMocks
    private AutofillService autofillService;

    private User user;
    private VaultEntry githubEntry;
    private VaultEntry googleEntry;
    private VaultEntry sensitiveEntry;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("testuser").email("t@t.com")
                .masterPasswordHash("h").salt("s").build();

        githubEntry = VaultEntry.builder().id(1L).user(user).title("GitHub")
                .username("enc_user").password("enc_pass")
                .websiteUrl("https://github.com").isDeleted(false).isHighlySensitive(false)
                .isFavorite(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        googleEntry = VaultEntry.builder().id(2L).user(user).title("Google")
                .username("enc_user2").password("enc_pass2")
                .websiteUrl("https://google.com").isDeleted(false).isHighlySensitive(false)
                .isFavorite(false).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        sensitiveEntry = VaultEntry.builder().id(3L).user(user).title("Bank")
                .username("enc_user3").password("enc_pass3")
                .websiteUrl("https://bank.com").isDeleted(false).isHighlySensitive(true)
                .isFavorite(false).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    // ── getSuggestions ────────────────────────────────────────────────────────

    @Test
    void getSuggestions_ExactMatch_ShouldReturnMatchingEntry() {
        AutofillSuggestionRequest request = AutofillSuggestionRequest.builder()
                .url("https://github.com/login").build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L))
                .thenReturn(List.of(githubEntry, googleEntry));
        when(domainMatchingService.extractDomain("https://github.com/login")).thenReturn("github.com");
        when(domainMatchingService.getMatchType("github.com", "https://github.com"))
                .thenReturn(DomainMatchingService.MatchType.EXACT);
        when(domainMatchingService.getMatchType("github.com", "https://google.com"))
                .thenReturn(DomainMatchingService.MatchType.NO_MATCH);
        // matchScore returns 0 by default from Mockito — sufficient for single-entry sorting

        AutofillSuggestionResponse response = autofillService.getSuggestions("testuser", request);

        assertThat(response.getDomain()).isEqualTo("github.com");
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getSuggestions()).hasSize(1);
        assertThat(response.getSuggestions().get(0).getEntryId()).isEqualTo(1L);
        assertThat(response.getSuggestions().get(0).getTitle()).isEqualTo("GitHub");
        assertThat(response.getSuggestions().get(0).getMatchType()).isEqualTo("EXACT");
        assertThat(response.getSuggestions().get(0).getIsFavorite()).isTrue();
    }

    @Test
    void getSuggestions_HighlySensitiveEntry_ShouldBeExcluded() {
        AutofillSuggestionRequest request = AutofillSuggestionRequest.builder()
                .url("https://bank.com").build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L))
                .thenReturn(List.of(sensitiveEntry));
        when(domainMatchingService.extractDomain("https://bank.com")).thenReturn("bank.com");

        AutofillSuggestionResponse response = autofillService.getSuggestions("testuser", request);

        assertThat(response.getTotalCount()).isEqualTo(0);
        assertThat(response.getSuggestions()).isEmpty();
        // getMatchType should never be called for sensitive entries
        verify(domainMatchingService, never()).getMatchType(any(), any());
    }

    @Test
    void getSuggestions_EntryWithoutUrl_ShouldBeExcluded() {
        VaultEntry noUrlEntry = VaultEntry.builder().id(4L).user(user).title("NoUrl")
                .username("u").password("p").websiteUrl(null)
                .isDeleted(false).isHighlySensitive(false).build();

        AutofillSuggestionRequest request = AutofillSuggestionRequest.builder()
                .url("https://github.com").build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(List.of(noUrlEntry));
        when(domainMatchingService.extractDomain("https://github.com")).thenReturn("github.com");

        AutofillSuggestionResponse response = autofillService.getSuggestions("testuser", request);

        assertThat(response.getTotalCount()).isEqualTo(0);
    }

    @Test
    void getSuggestions_MultipleMatches_ShouldSortByMatchScore() {
        VaultEntry subdomainEntry = VaultEntry.builder().id(5L).user(user).title("GitHub App")
                .username("u").password("p").websiteUrl("https://app.github.com")
                .isDeleted(false).isHighlySensitive(false).isFavorite(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        AutofillSuggestionRequest request = AutofillSuggestionRequest.builder()
                .url("https://github.com").build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L))
                .thenReturn(List.of(subdomainEntry, githubEntry));
        when(domainMatchingService.extractDomain("https://github.com")).thenReturn("github.com");
        when(domainMatchingService.getMatchType("github.com", "https://app.github.com"))
                .thenReturn(DomainMatchingService.MatchType.SUBDOMAIN);
        when(domainMatchingService.getMatchType("github.com", "https://github.com"))
                .thenReturn(DomainMatchingService.MatchType.EXACT);
        when(domainMatchingService.matchScore(DomainMatchingService.MatchType.EXACT)).thenReturn(3);
        when(domainMatchingService.matchScore(DomainMatchingService.MatchType.SUBDOMAIN)).thenReturn(2);

        AutofillSuggestionResponse response = autofillService.getSuggestions("testuser", request);

        assertThat(response.getTotalCount()).isEqualTo(2);
        // EXACT should come first
        assertThat(response.getSuggestions().get(0).getMatchType()).isEqualTo("EXACT");
        assertThat(response.getSuggestions().get(1).getMatchType()).isEqualTo("SUBDOMAIN");
    }

    @Test
    void getSuggestions_EmptyVault_ShouldReturnEmptyList() {
        AutofillSuggestionRequest request = AutofillSuggestionRequest.builder()
                .url("https://github.com").build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(List.of());
        when(domainMatchingService.extractDomain("https://github.com")).thenReturn("github.com");

        AutofillSuggestionResponse response = autofillService.getSuggestions("testuser", request);

        assertThat(response.getTotalCount()).isEqualTo(0);
        assertThat(response.getSuggestions()).isEmpty();
    }

    // ── getTrustedDomains ─────────────────────────────────────────────────────

    @Test
    void getTrustedDomains_ShouldReturnDistinctDomains() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(usageLogRepository.findDistinctDomainsByUserId(1L))
                .thenReturn(List.of("github.com", "google.com"));

        List<String> domains = autofillService.getTrustedDomains("testuser");

        assertThat(domains).containsExactly("github.com", "google.com");
    }

    @Test
    void getTrustedDomains_NoUsage_ShouldReturnEmptyList() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(usageLogRepository.findDistinctDomainsByUserId(1L)).thenReturn(List.of());

        List<String> domains = autofillService.getTrustedDomains("testuser");

        assertThat(domains).isEmpty();
    }

    // ── logUsage ──────────────────────────────────────────────────────────────

    @Test
    void logUsage_Applied_ShouldSaveLogWithAppliedTrue() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(domainMatchingService.extractDomain("https://github.com/login")).thenReturn("github.com");
        when(usageLogRepository.save(any(AutofillUsageLog.class))).thenAnswer(inv -> inv.getArgument(0));

        autofillService.logUsage("testuser", "https://github.com/login", 1L, true);

        ArgumentCaptor<AutofillUsageLog> captor = ArgumentCaptor.forClass(AutofillUsageLog.class);
        verify(usageLogRepository).save(captor.capture());
        AutofillUsageLog saved = captor.getValue();
        assertThat(saved.getDomain()).isEqualTo("github.com");
        assertThat(saved.getVaultEntryId()).isEqualTo(1L);
        assertThat(saved.getApplied()).isTrue();
    }

    @Test
    void logUsage_NotApplied_ShouldSaveLogWithAppliedFalse() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(domainMatchingService.extractDomain("https://github.com")).thenReturn("github.com");
        when(usageLogRepository.save(any(AutofillUsageLog.class))).thenAnswer(inv -> inv.getArgument(0));

        autofillService.logUsage("testuser", "https://github.com", null, false);

        ArgumentCaptor<AutofillUsageLog> captor = ArgumentCaptor.forClass(AutofillUsageLog.class);
        verify(usageLogRepository).save(captor.capture());
        assertThat(captor.getValue().getApplied()).isFalse();
        assertThat(captor.getValue().getVaultEntryId()).isNull();
    }
}
