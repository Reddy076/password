package com.revature.passwordmanager.service.emergency;

import com.revature.passwordmanager.dto.request.AddEmergencyContactRequest;
import com.revature.passwordmanager.dto.response.EmergencyContactResponse;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.emergency.EmergencyContact;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.EmergencyContactRepository;
import com.revature.passwordmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 *
 * <p>Manages the vault owner's list of emergency contacts.</p>
 */
@Service
@RequiredArgsConstructor
public class EmergencyContactService {

    private final UserRepository userRepository;
    private final EmergencyContactRepository contactRepository;

    // ── Add contact ───────────────────────────────────────────────────────────

    /**
     * Adds a new emergency contact for the authenticated vault owner.
     * A user cannot add themselves as an emergency contact.
     * Duplicate contacts (same email) are rejected.
     *
     * @param ownerUsername the authenticated vault owner
     * @param request       the contact details
     * @return the created contact response
     */
    @Transactional
    public EmergencyContactResponse addContact(String ownerUsername, AddEmergencyContactRequest request) {
        User owner = userRepository.findByUsernameOrThrow(ownerUsername);

        // Validate email is provided for add
        if (request.getContactEmail() == null || request.getContactEmail().isBlank()) {
            throw new IllegalArgumentException("Contact email is required");
        }

        // Cannot add yourself
        if (owner.getEmail().equalsIgnoreCase(request.getContactEmail())) {
            throw new IllegalArgumentException("You cannot add yourself as an emergency contact");
        }

        // Check for duplicate
        contactRepository.findByUserIdAndContactEmailAndActiveTrue(owner.getId(), request.getContactEmail())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "An emergency contact with email '" + request.getContactEmail() + "' already exists");
                });

        int waitingHours = request.getWaitingPeriodHours() != null ? request.getWaitingPeriodHours() : 48;

        EmergencyContact contact = EmergencyContact.builder()
                .user(owner)
                .contactEmail(request.getContactEmail())
                .contactName(request.getContactName())
                .relationship(request.getRelationship())
                .waitingPeriodHours(waitingHours)
                .verified(false)
                .active(true)
                .build();

        contact = contactRepository.save(contact);
        return mapToResponse(contact);
    }

    // ── List contacts ─────────────────────────────────────────────────────────

    /**
     * Returns all active emergency contacts for the vault owner.
     */
    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> getContacts(String ownerUsername) {
        User owner = userRepository.findByUsernameOrThrow(ownerUsername);
        return contactRepository.findByUserIdAndActiveTrue(owner.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Remove contact ────────────────────────────────────────────────────────

    /**
     * Soft-deletes (deactivates) an emergency contact.
     *
     * @param ownerUsername the authenticated vault owner
     * @param contactId     the contact to remove
     */
    @Transactional
    public void removeContact(String ownerUsername, Long contactId) {
        User owner = userRepository.findByUsernameOrThrow(ownerUsername);
        EmergencyContact contact = contactRepository.findByIdAndUserId(contactId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Emergency contact not found"));

        contact.setActive(false);
        contactRepository.save(contact);
    }

    // ── Update contact ────────────────────────────────────────────────────────

    /**
     * Updates an existing emergency contact's details.
     *
     * @param ownerUsername the authenticated vault owner
     * @param contactId     the contact to update
     * @param request       the updated details (null fields are ignored)
     * @return the updated contact response
     */
    @Transactional
    public EmergencyContactResponse updateContact(String ownerUsername, Long contactId,
                                                   AddEmergencyContactRequest request) {
        User owner = userRepository.findByUsernameOrThrow(ownerUsername);
        EmergencyContact contact = contactRepository.findByIdAndUserId(contactId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Emergency contact not found"));

        if (!Boolean.TRUE.equals(contact.getActive())) {
            throw new ResourceNotFoundException("Emergency contact not found");
        }

        if (request.getContactName() != null) {
            contact.setContactName(request.getContactName());
        }
        if (request.getRelationship() != null) {
            contact.setRelationship(request.getRelationship());
        }
        if (request.getWaitingPeriodHours() != null) {
            contact.setWaitingPeriodHours(request.getWaitingPeriodHours());
        }

        contact = contactRepository.save(contact);
        return mapToResponse(contact);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    public EmergencyContactResponse mapToResponse(EmergencyContact contact) {
        return EmergencyContactResponse.builder()
                .id(contact.getId())
                .contactEmail(contact.getContactEmail())
                .contactName(contact.getContactName())
                .relationship(contact.getRelationship())
                .waitingPeriodHours(contact.getWaitingPeriodHours())
                .verified(contact.getVerified())
                .active(contact.getActive())
                .createdAt(contact.getCreatedAt())
                .build();
    }
}
