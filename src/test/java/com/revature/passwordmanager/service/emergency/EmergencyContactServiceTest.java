package com.revature.passwordmanager.service.emergency;

import com.revature.passwordmanager.dto.request.AddEmergencyContactRequest;
import com.revature.passwordmanager.dto.response.EmergencyContactResponse;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.emergency.EmergencyContact;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.EmergencyContactRepository;
import com.revature.passwordmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 */
@ExtendWith(MockitoExtension.class)
class EmergencyContactServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmergencyContactRepository contactRepository;

    @InjectMocks
    private EmergencyContactService contactService;

    private User owner;
    private EmergencyContact contact;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .username("owner")
                .email("owner@example.com")
                .masterPasswordHash("hash")
                .salt("salt")
                .build();

        contact = EmergencyContact.builder()
                .id(10L)
                .user(owner)
                .contactEmail("contact@example.com")
                .contactName("John Doe")
                .relationship("Spouse")
                .waitingPeriodHours(48)
                .verified(false)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── addContact ────────────────────────────────────────────────────────────

    @Test
    void addContact_ValidRequest_ShouldCreateAndReturnContact() {
        AddEmergencyContactRequest request = AddEmergencyContactRequest.builder()
                .contactEmail("contact@example.com")
                .contactName("John Doe")
                .relationship("Spouse")
                .waitingPeriodHours(48)
                .build();

        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(contactRepository.findByUserIdAndContactEmailAndActiveTrue(1L, "contact@example.com"))
                .thenReturn(Optional.empty());
        when(contactRepository.save(any(EmergencyContact.class))).thenReturn(contact);

        EmergencyContactResponse response = contactService.addContact("owner", request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getContactEmail()).isEqualTo("contact@example.com");
        assertThat(response.getContactName()).isEqualTo("John Doe");
        assertThat(response.getRelationship()).isEqualTo("Spouse");
        assertThat(response.getWaitingPeriodHours()).isEqualTo(48);
        assertThat(response.getVerified()).isFalse();
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void addContact_DefaultWaitingPeriod_ShouldUse48Hours() {
        AddEmergencyContactRequest request = AddEmergencyContactRequest.builder()
                .contactEmail("contact@example.com")
                .build();

        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(contactRepository.findByUserIdAndContactEmailAndActiveTrue(1L, "contact@example.com"))
                .thenReturn(Optional.empty());
        when(contactRepository.save(any(EmergencyContact.class))).thenReturn(contact);

        contactService.addContact("owner", request);

        ArgumentCaptor<EmergencyContact> captor = ArgumentCaptor.forClass(EmergencyContact.class);
        verify(contactRepository).save(captor.capture());
        assertThat(captor.getValue().getWaitingPeriodHours()).isEqualTo(48);
    }

    @Test
    void addContact_SelfEmail_ShouldThrowIllegalArgumentException() {
        AddEmergencyContactRequest request = AddEmergencyContactRequest.builder()
                .contactEmail("owner@example.com") // same as owner's email
                .build();

        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);

        assertThatThrownBy(() -> contactService.addContact("owner", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot add yourself");
    }

    @Test
    void addContact_DuplicateEmail_ShouldThrowIllegalArgumentException() {
        AddEmergencyContactRequest request = AddEmergencyContactRequest.builder()
                .contactEmail("contact@example.com")
                .build();

        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(contactRepository.findByUserIdAndContactEmailAndActiveTrue(1L, "contact@example.com"))
                .thenReturn(Optional.of(contact));

        assertThatThrownBy(() -> contactService.addContact("owner", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    // ── getContacts ───────────────────────────────────────────────────────────

    @Test
    void getContacts_ShouldReturnAllActiveContacts() {
        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(contactRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of(contact));

        List<EmergencyContactResponse> responses = contactService.getContacts("owner");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getContactEmail()).isEqualTo("contact@example.com");
    }

    @Test
    void getContacts_NoContacts_ShouldReturnEmptyList() {
        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(contactRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of());

        List<EmergencyContactResponse> responses = contactService.getContacts("owner");

        assertThat(responses).isEmpty();
    }

    // ── removeContact ─────────────────────────────────────────────────────────

    @Test
    void removeContact_ValidContact_ShouldDeactivate() {
        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(contactRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(contact));
        when(contactRepository.save(any())).thenReturn(contact);

        contactService.removeContact("owner", 10L);

        ArgumentCaptor<EmergencyContact> captor = ArgumentCaptor.forClass(EmergencyContact.class);
        verify(contactRepository).save(captor.capture());
        assertThat(captor.getValue().getActive()).isFalse();
    }

    @Test
    void removeContact_NotFound_ShouldThrowResourceNotFoundException() {
        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(contactRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactService.removeContact("owner", 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateContact ─────────────────────────────────────────────────────────

    @Test
    void updateContact_ValidRequest_ShouldUpdateFields() {
        AddEmergencyContactRequest request = AddEmergencyContactRequest.builder()
                .contactName("Jane Doe")
                .relationship("Parent")
                .waitingPeriodHours(72)
                .build();

        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(contactRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(contact));
        when(contactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmergencyContactResponse response = contactService.updateContact("owner", 10L, request);

        assertThat(response.getContactName()).isEqualTo("Jane Doe");
        assertThat(response.getRelationship()).isEqualTo("Parent");
        assertThat(response.getWaitingPeriodHours()).isEqualTo(72);
    }

    @Test
    void updateContact_NullFields_ShouldNotOverwriteExistingValues() {
        AddEmergencyContactRequest request = AddEmergencyContactRequest.builder()
                .contactName("Jane Doe")
                // relationship and waitingPeriodHours are null
                .build();

        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(contactRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(contact));
        when(contactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmergencyContactResponse response = contactService.updateContact("owner", 10L, request);

        assertThat(response.getContactName()).isEqualTo("Jane Doe");
        assertThat(response.getRelationship()).isEqualTo("Spouse"); // unchanged
        assertThat(response.getWaitingPeriodHours()).isEqualTo(48); // unchanged
    }
}
