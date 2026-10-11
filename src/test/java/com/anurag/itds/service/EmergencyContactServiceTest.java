package com.anurag.itds.service;

import com.anurag.itds.dto.EmergencyContactRequest;
import com.anurag.itds.entity.EmergencyContact;
import com.anurag.itds.entity.User;
import com.anurag.itds.exception.BadRequestException;
import com.anurag.itds.exception.ConflictException;
import com.anurag.itds.exception.ForbiddenException;
import com.anurag.itds.exception.ResourceNotFoundException;
import com.anurag.itds.repository.EmergencyContactRepository;
import com.anurag.itds.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmergencyContactServiceTest {

    private final EmergencyContactRepository contacts = mock(EmergencyContactRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final EmergencyContactService service = new EmergencyContactService(contacts, users);

    private static User user(long id) {
        User u = new User("U" + id, "u" + id + "@x.test", "90000000" + id, "hash");
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private static EmergencyContact contact(long id, User owner, String phone) {
        EmergencyContact c = new EmergencyContact(owner, "Mom", phone, "Mother");
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    @Test
    void createTrimsFieldsAndSaves() {
        User u = user(1);
        when(users.findById(1L)).thenReturn(Optional.of(u));
        when(contacts.save(any(EmergencyContact.class))).thenAnswer(inv -> inv.getArgument(0));
        var resp = service.createOwn(1L, new EmergencyContactRequest(" Mom ", " 9123456789 ", " Mother "));
        assertThat(resp.name()).isEqualTo("Mom");
        assertThat(resp.phone()).isEqualTo("9123456789");
        assertThat(resp.relation()).isEqualTo("Mother");
    }

    @Test
    void createDuplicatePhoneIsConflict() {
        when(users.findById(1L)).thenReturn(Optional.of(user(1)));
        when(contacts.existsByUserIdAndPhone(1L, "9123456789")).thenReturn(true);
        assertThatThrownBy(() -> service.createOwn(1L, new EmergencyContactRequest("Mom", "9123456789", "Mother")))
                .isInstanceOf(ConflictException.class);
        verify(contacts, never()).save(any());
    }

    @Test
    void createForMissingUserIsNotFound() {
        when(users.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createOwn(9L, new EmergencyContactRequest("Mom", "9123456789", "Mother")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAnotherUsersContactIsForbidden() {
        when(contacts.findById(5L)).thenReturn(Optional.of(contact(5, user(2), "9123456789")));
        assertThatThrownBy(() -> service.updateOwn(1L, 5L, new EmergencyContactRequest("X", "9123456780", "Friend")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteBlockedAtMinimumAndAllowedAboveIt() {
        User u = user(1);
        EmergencyContact c = contact(5, u, "9123456789");
        when(contacts.findById(5L)).thenReturn(Optional.of(c));

        when(contacts.countByUserId(1L)).thenReturn((long) EmergencyContactService.MIN_CONTACTS);
        assertThatThrownBy(() -> service.deleteOwn(1L, 5L)).isInstanceOf(BadRequestException.class);
        verify(contacts, never()).delete(any());

        when(contacts.countByUserId(1L)).thenReturn((long) EmergencyContactService.MIN_CONTACTS + 1);
        service.deleteOwn(1L, 5L);
        verify(contacts).delete(c);
    }
}
