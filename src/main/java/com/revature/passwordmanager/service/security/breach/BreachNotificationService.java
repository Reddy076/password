package com.revature.passwordmanager.service.security.breach;

import com.revature.passwordmanager.model.security.SecurityAlert.AlertType;
import com.revature.passwordmanager.model.security.SecurityAlert.Severity;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.service.security.SecurityAlertService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BreachNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(BreachNotificationService.class);

    private final SecurityAlertService securityAlertService;

    /**
     * Creates a security alert and in-app notification when a vault entry
     * password is found in a known data breach.
     *
     * @param username   the authenticated user's username
     * @param entry      the vault entry whose password is compromised
     * @param pwnedCount number of times the password appeared in breaches
     */
    public void notifyBreach(String username, VaultEntry entry, long pwnedCount) {
        String title = "Password Breached: " + entry.getTitle();
        String message = String.format(
                "The password for '%s' (%s) has appeared in %s data breach(es). " +
                "It has been seen %,d time(s) in compromised datasets. " +
                "Please change this password immediately.",
                entry.getTitle(),
                entry.getWebsiteUrl() != null ? entry.getWebsiteUrl() : "no URL",
                pwnedCount > 10000 ? "major" : "known",
                pwnedCount);

        Severity severity = pwnedCount > 10000 ? Severity.CRITICAL : Severity.HIGH;

        try {
            securityAlertService.createAlert(username, AlertType.PASSWORD_BREACHED,
                    title, message, severity);
            logger.info("Breach notification created for user={} entry={} pwnedCount={}",
                    username, entry.getId(), pwnedCount);
        } catch (Exception e) {
            logger.error("Failed to create breach notification for user={} entry={}: {}",
                    username, entry.getId(), e.getMessage());
        }
    }

    /**
     * Creates a summary notification after a full vault scan completes.
     *
     * @param username         the user
     * @param compromisedCount how many entries were compromised
     * @param totalScanned     how many entries were scanned
     */
    public void notifyScanComplete(String username, int compromisedCount, int totalScanned) {
        if (compromisedCount == 0) {
            securityAlertService.createAlert(
                    username,
                    AlertType.PASSWORD_BREACHED,
                    "Breach Scan Complete — All Clear",
                    String.format("Scanned %d password(s). No breached passwords found. Your vault looks safe!",
                            totalScanned),
                    Severity.LOW);
        } else {
            securityAlertService.createAlert(
                    username,
                    AlertType.PASSWORD_BREACHED,
                    String.format("Breach Scan Complete — %d Password(s) Compromised", compromisedCount),
                    String.format("Scanned %d password(s) and found %d compromised. " +
                            "Please update the affected passwords immediately.",
                            totalScanned, compromisedCount),
                    compromisedCount > 3 ? Severity.CRITICAL : Severity.HIGH);
        }
    }
}
