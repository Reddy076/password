package com.revature.passwordmanager.service.emergency;

import com.revature.passwordmanager.model.emergency.EmergencyAccessRequest;
import com.revature.passwordmanager.model.emergency.EmergencyContact;
import com.revature.passwordmanager.model.notification.Notification.NotificationType;
import com.revature.passwordmanager.service.email.EmailService;
import com.revature.passwordmanager.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 *
 * <p>Sends email and in-app notifications for emergency access events.</p>
 */
@Service
@RequiredArgsConstructor
public class EmergencyNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyNotificationService.class);

    private final NotificationService notificationService;
    private final EmailService emailService;

    /**
     * Notifies the vault owner that an emergency contact has requested access.
     * Sends both an in-app notification and an email.
     *
     * @param request the newly created access request
     */
    public void notifyOwnerOfAccessRequest(EmergencyAccessRequest request) {
        EmergencyContact contact = request.getContact();
        String ownerUsername = request.getUser().getUsername();
        String ownerEmail = request.getUser().getEmail();
        String contactName = contact.getContactName() != null ? contact.getContactName() : contact.getContactEmail();

        String title = "Emergency Access Requested";
        String message = String.format(
                "%s (%s) has requested emergency access to your vault. " +
                "Access will be automatically granted in %d hour(s) unless you deny it.",
                contactName, contact.getContactEmail(), request.getWaitingPeriodHours());

        notificationService.createNotification(ownerUsername, NotificationType.SECURITY_ALERT, title, message);

        try {
            emailService.sendSimpleEmail(ownerEmail, title, message);
        } catch (Exception e) {
            logger.warn("Failed to send emergency access email to {}: {}", ownerEmail, e.getMessage());
        }
    }

    /**
     * Notifies the vault owner that an emergency access request was auto-approved.
     *
     * @param request the approved request
     */
    public void notifyOwnerOfAutoApproval(EmergencyAccessRequest request) {
        String ownerUsername = request.getUser().getUsername();
        EmergencyContact contact = request.getContact();
        String contactName = contact.getContactName() != null ? contact.getContactName() : contact.getContactEmail();

        String title = "Emergency Access Granted";
        String message = String.format(
                "Emergency access has been automatically granted to %s (%s) " +
                "because the waiting period elapsed without a denial.",
                contactName, contact.getContactEmail());

        notificationService.createNotification(ownerUsername, NotificationType.SECURITY_ALERT, title, message);
    }

    /**
     * Notifies the vault owner that they successfully denied an emergency access request.
     *
     * @param request the denied request
     */
    public void notifyOwnerOfDenial(EmergencyAccessRequest request) {
        String ownerUsername = request.getUser().getUsername();
        EmergencyContact contact = request.getContact();
        String contactName = contact.getContactName() != null ? contact.getContactName() : contact.getContactEmail();

        String title = "Emergency Access Denied";
        String message = String.format(
                "You have denied the emergency access request from %s (%s).",
                contactName, contact.getContactEmail());

        notificationService.createNotification(ownerUsername, NotificationType.ACCOUNT_ACTIVITY, title, message);
    }
}
