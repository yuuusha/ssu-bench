package com.diev.service;

import com.diev.entity.Role;
import com.diev.entity.User;
import com.diev.exception.BadRequestException;
import com.diev.exception.ConflictException;
import com.diev.exception.ErrorCode;
import com.diev.exception.NotFoundException;
import com.diev.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserAccessService accessService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, accessService);
    }

    @Test
    void createUserStoresEncodedPassword() {
        String email = "new@example.com";
        String rawPassword = "password123";
        Role role = Role.EXECUTOR;

        when(userRepository.findById(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID generatedId = invocation.getArgument(0);
                    return Optional.of(new User(generatedId, email, "encoded", role.name(), 0, false));
                });

        User user = userService.createUser(email, rawPassword, role);

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(userRepository).create(any(UUID.class), eq(email), passwordCaptor.capture(), eq(role.name()), eq(0L), eq(false));

        assertEquals(email, user.getEmail());
        assertEquals(role.name(), user.getRole());
        assertTrue(new BCryptPasswordEncoder().matches(rawPassword, passwordCaptor.getValue()));
    }

    @Test
    void getUserReturnsUserForOwnerOrAdmin() {
        UUID id = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        User user = new User(id, "test@example.com", "hash", "CUSTOMER", 0, false);

        doNothing().when(accessService).requireOwnerOrAdmin(currentUserId, id, ErrorCode.ONLY_OWNER_OR_ADMIN_CAN_VIEW_USER);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        User result = userService.getUser(id, currentUserId);

        assertSame(user, result);
    }

    @Test
    void updateUserUpdatesExistingUser() {
        UUID id = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        String newEmail = "updated@example.com";
        String rawPassword = "newpass";
        Role role = Role.ADMIN;
        long newBalance = 10;

        User existing = new User(id, "old@example.com", "oldHash", "CUSTOMER", 5, false);
        User updated = new User(id, newEmail, "newHash", role.name(), 5, false);

        doNothing().when(accessService).requireOwnerOrAdmin(currentUserId, id, ErrorCode.ONLY_OWNER_OR_ADMIN_CAN_UPDATE_USER);
        when(userRepository.findById(id)).thenReturn(Optional.of(existing), Optional.of(updated));

        User result = userService.updateUser(id, currentUserId, newEmail, rawPassword, role, newBalance);

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(userRepository).update(eq(id), eq(newEmail), passwordCaptor.capture(), eq(role.name()), eq(newBalance));

        assertEquals(newEmail, result.getEmail());
        assertEquals(role.name(), result.getRole());
        assertTrue(new BCryptPasswordEncoder().matches(rawPassword, passwordCaptor.getValue()));
    }

    @Test
    void updateUserBalanceUpdatesBalanceForAdminOnly() {
        UUID id = UUID.randomUUID();

        User existing = new User(id, "test@example.com", "hash", "CUSTOMER", 5, false);
        User updated = new User(id, "test@example.com", "hash", "CUSTOMER", 25, false);

        when(userRepository.findById(id)).thenReturn(Optional.of(existing), Optional.of(updated));

        User result = userService.updateUserBalance(id, 25);

        verify(userRepository).updateBalance(id, 25L);
        assertEquals(25, result.getBalance());
    }

    @Test
    void updateUserBalanceThrowsWhenInvalidValue() {
        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.of(new User(id, "test@example.com", "hash", "CUSTOMER", 5, false)));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.updateUserBalance(id, 0));

        assertEquals("Balance must be greater than zero.", ex.getMessage());
        verify(userRepository, never()).updateBalance(any(), anyLong());
    }

    @Test
    void deleteUserDeletesExistingUserForAdmin() {
        UUID id = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        when(userRepository.findById(id))
                .thenReturn(Optional.of(new User(id, "test@example.com", "hash", "CUSTOMER", 0, false)));

        assertDoesNotThrow(() -> userService.deleteUser(id, currentUserId));

        verify(userRepository).delete(id);
    }

    @Test
    void getAllUsersReturnsPagedListForAdmin() {

        List<User> users = List.of(
                new User(UUID.randomUUID(), "a@example.com", "hash", "CUSTOMER", 0, false),
                new User(UUID.randomUUID(), "b@example.com", "hash", "EXECUTOR", 10, false)
        );

        when(userRepository.findAll(20, 0)).thenReturn(users);

        List<User> result = userService.getAllUsers(20, 0);

        assertEquals(2, result.size());
        assertEquals(users, result);
    }

    @Test
    void blockAndUnblockUserWorkForAdmin() {
        UUID id = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        when(userRepository.findById(id))
                .thenReturn(Optional.of(new User(id, "test@example.com", "hash", "CUSTOMER", 0, false)))
                .thenReturn(Optional.of(new User(id, "test@example.com", "hash", "CUSTOMER", 0, true)))
                .thenReturn(Optional.of(new User(id, "test@example.com", "hash", "CUSTOMER", 0, false)));

        userService.blockUser(id, adminId);
        userService.unblockUser(id);

        verify(userRepository).block(id);
        verify(userRepository).unblock(id);
    }

    @Test
    void blockUserWhenAdminBlocksHimselfThrowsException() {
        UUID id = UUID.randomUUID();

        ConflictException ex = assertThrows(ConflictException.class, () -> userService.blockUser(id, id));
        assertEquals("Admin cannot block himself", ex.getMessage());
    }

    @Test
    void getUserThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        doNothing().when(accessService).requireOwnerOrAdmin(currentUserId, id, ErrorCode.ONLY_OWNER_OR_ADMIN_CAN_VIEW_USER);
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> userService.getUser(id, currentUserId));
        assertEquals("User not found.", ex.getMessage());
    }
}