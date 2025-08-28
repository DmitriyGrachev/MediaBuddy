package org.hrachov.com.filmproject.service.admin;

import org.hrachov.com.filmproject.exception.BlockUserException;
import org.hrachov.com.filmproject.exception.IlligalRoleException;
import org.hrachov.com.filmproject.exception.UserNotFoundException;
import org.hrachov.com.filmproject.model.Role;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.dto.BlockUserRequest;
import org.hrachov.com.filmproject.model.dto.UserDTO;
import org.hrachov.com.filmproject.model.dto.UserResponseDTO;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.service.BlockService;
import org.hrachov.com.filmproject.service.impl.admin.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserManagementServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private BlockService blockService;

    @InjectMocks
    private UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        userManagementService = new UserManagementService(userRepository, blockService);
    }

    @Test
    @DisplayName("Get all users with block info of both users")
    void getAllUsersWithBlockInfo_ShouldReturnListOfUsersResponseDto() {
        User user1 = new User();
        user1.setId(1L);
        user1.setRoles(new HashSet<>(Arrays.asList(Role.ROLE_REGULAR)));
        User user2 = new User();
        user2.setId(2L);
        user2.setRoles(new HashSet<>(Arrays.asList(Role.ROLE_REGULAR)));

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));
        when(blockService.isUserBlocked(1L)).thenReturn(true);
        when(blockService.isUserBlocked(2L)).thenReturn(true);
        when(blockService.getFullBlockInfo(anyLong())).thenReturn(Map.of(
                "reason", "User blocked",
                "blockedUntil", "2025-09-19T13:57",
                "blockedAt", "2025-07-19T13:57:51.209228100"
        ));

        UserResponseDTO userResponseDTO1 = new UserResponseDTO(user1, true, "User blocked", "2025-09-19T13:57", "2025-07-19T13:57:51.209228100");
        UserResponseDTO userResponseDTO2 = new UserResponseDTO(user2, true, "User blocked", "2025-09-19T13:57", "2025-07-19T13:57:51.209228100");
        List<UserResponseDTO> userResponseDTOListExpected = List.of(userResponseDTO1, userResponseDTO2);

        List<UserResponseDTO> userResponseDTOListActual = userManagementService.getAllUsersWithBlockInfo();
        assertEquals(userResponseDTOListExpected.size(), userResponseDTOListActual.size());
        assertEquals(userResponseDTOListExpected.get(0), userResponseDTOListActual.get(0));
        assertEquals(userResponseDTOListExpected.get(1), userResponseDTOListActual.get(1));

        verify(userRepository, times(1)).findAll();
        verify(blockService, times(1)).isUserBlocked(1L);
        verify(blockService, times(1)).isUserBlocked(2L);
        verify(blockService, times(2)).getFullBlockInfo(anyLong());
    }

    @Test
    @DisplayName("Get all blocked users info with only one user out of two")
    void getAllUsersBlockedInfo_ShouldReturnListOfUsersResponseDtoWithOneUserOutOfTwo() {
        User user1 = new User();
        user1.setId(1L);
        user1.setRoles(new HashSet<>(Arrays.asList(Role.ROLE_REGULAR)));
        User user2 = new User();
        user2.setId(2L);
        user2.setRoles(new HashSet<>(Arrays.asList(Role.ROLE_REGULAR)));

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));
        when(blockService.isUserBlocked(1L)).thenReturn(true);
        when(blockService.isUserBlocked(2L)).thenReturn(false);
        when(blockService.getFullBlockInfo(anyLong())).thenReturn(Map.of(
                "reason", "User blocked",
                "blockedUntil", "2025-09-19T13:57",
                "blockedAt", "2025-07-19T13:57:51.209228100"
        ));

        UserResponseDTO userResponseDTO1 = new UserResponseDTO(user1, true, "User blocked", "2025-09-19T13:57", "2025-07-19T13:57:51.209228100");
        UserResponseDTO userResponseDTO2 = new UserResponseDTO(user2, false, "User blocked", "2025-09-19T13:57", "2025-07-19T13:57:51.209228100");
        List<UserResponseDTO> userResponseDTOListExpected = List.of(userResponseDTO1, userResponseDTO2);

        List<UserResponseDTO> userResponseDTOListActual = userManagementService.getAllUsersWithBlockInfo();
        assertEquals(userResponseDTOListExpected.size(), userResponseDTOListActual.size());
        assertEquals(userResponseDTOListExpected.get(0).isBlocked(), true);
        assertEquals(userResponseDTOListExpected.get(1).isBlocked(), false);

        verify(userRepository, times(1)).findAll();
        verify(blockService, times(1)).isUserBlocked(1L);
        verify(blockService, times(1)).isUserBlocked(2L);
        verify(blockService, times(1)).getFullBlockInfo(anyLong());
    }

    @Test
    @DisplayName("Delete user by id should return user success")
    void deleteUserByIdShouldReturnUserUponSuccess() {
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("admin");
        user1.setRoles(new HashSet<>(Arrays.asList(Role.ROLE_REGULAR)));

        when(userRepository.getUserById(1L)).thenReturn(user1);

        User deletedUser = userManagementService.deleteUserById(1L);
        verify(userRepository, times(1)).delete(user1);
        verify(userRepository, times(1)).getUserById(1L);
        assertEquals(1L, deletedUser.getId());
        assertEquals("admin", deletedUser.getUsername());
    }

    @Test
    @DisplayName("Delete user by id should throw UserNotFoundException")
    void deleteUserByIdShouldThrowUserNotFoundException() {
        when(userRepository.getUserById(1L)).thenReturn(null);
        UserNotFoundException userNotFoundException = assertThrows(
                UserNotFoundException.class,
                () -> userManagementService.deleteUserById(1L)
        );

        verify(userRepository, never()).delete(any(User.class));
        assertEquals(userNotFoundException.getMessage(), "User with id 1 not found");
    }

    @Test
    @DisplayName("Update user by userDto")
    void updateUserByIdShouldReturnUserUponSuccess() {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("admin");

        User user = new User();
        user.setId(1L);
        user.setUsername("SHOULD_CHANGE");

        when(userRepository.getUserById(1L)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        User userActual = userManagementService.updateUser(userDTO);

        assertEquals(userDTO.getUsername(), userActual.getUsername());
        verify(userRepository, times(1)).getUserById(1L);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Update user should throw user not found exception")
    void updateUserShouldThrowUserNotFoundException() {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);

        when(userRepository.getUserById(1L)).thenReturn(null);
        UserNotFoundException userNotFoundException = assertThrows(
                UserNotFoundException.class,
                () -> userManagementService.updateUser(userDTO)
        );
        verify(userRepository, never()).save(any(User.class));
        assertEquals(userNotFoundException.getMessage(), "User with id 1 not found");
    }

    @Test
    @DisplayName("updateUserRoles должен успешно обновить роли, когда пользователь существует")
    void updateUserRoles_shouldUpdateRoles_whenUserExists() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setRoles(Set.of(Role.ROLE_REGULAR));
        Set<String> newRoleStrings = Set.of("ROLE_ADMIN", "ROLE_REGULAR");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userManagementService.updateUserRoles(userId, newRoleStrings);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(2, savedUser.getRoles().size());
        assertTrue(savedUser.getRoles().contains(Role.ROLE_ADMIN));
        assertTrue(savedUser.getRoles().contains(Role.ROLE_REGULAR));
    }

    @Test
    @DisplayName("updateUserRoles должен выбросить исключение, когда пользователь не найден")
    void updateUserRoles_shouldThrowException_whenUserNotFound() {
        Long userId = 99L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userManagementService.updateUserRoles(userId, Set.of("ROLE_ADMIN"))
        );

        assertEquals("User with id " + userId + " not found", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserRoles должен выбросить исключение при неверном названии роли")
    void updateUserRoles_shouldThrowException_whenRoleIsInvalid() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        Set<String> invalidRoleStrings = Set.of("ROLE_INVALID");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        IlligalRoleException exception = assertThrows(
                IlligalRoleException.class,
                () -> userManagementService.updateUserRoles(userId, invalidRoleStrings)
        );

        assertEquals(null, exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("blockUser должен вызвать временную блокировку, если указана дата")
    void blockUser_shouldCallBlockUser_whenDateIsProvided() {
        BlockUserRequest request = new BlockUserRequest();
        request.setUserId(1L);
        request.setReason("Test");
        request.setBlockedUntil(LocalDateTime.now().plusDays(1));

        userManagementService.blockUser(request);

        verify(blockService).blockUser(request.getUserId(), request.getReason(), request.getBlockedUntil());
        verify(blockService, never()).permanentBlockUser(anyLong(), anyString());
    }

    @Test
    @DisplayName("blockUser должен вызвать постоянную блокировку, если дата не указана")
    void blockUser_shouldCallPermanentBlock_whenDateIsNull() {
        BlockUserRequest request = new BlockUserRequest();
        request.setUserId(1L);
        request.setReason("Permanent ban");
        request.setBlockedUntil(null);

        userManagementService.blockUser(request);

        verify(blockService).permanentBlockUser(request.getUserId(), request.getReason());
        verify(blockService, never()).blockUser(anyLong(), anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("UnblockUser should succeed")
    void unblockUser_shouldSucceed() {
        userManagementService.unblockUser(1L);
        verify(blockService, times(1)).unblockUser(1L);
    }

    @Test
    @DisplayName("Exception should be thrown when UnblockUser")
    void unblockUser_shouldThrowException_whenUnblockUser() {
        doThrow(new BlockUserException("User either unblocked or not found in cache"))
                .when(blockService).unblockUser(1L);

        BlockUserException blockUserException = assertThrows(
                BlockUserException.class,
                () -> userManagementService.unblockUser(1L)
        );
        assertEquals("User either unblocked or not found in cache", blockUserException.getMessage());
    }

    @Test
    @DisplayName("Should return block info")
    void getBlockInfo_shouldReturnCorrectMap() {
        Long userId = 1L;

        when(blockService.isUserBlocked(userId)).thenReturn(true);
        when(blockService.getBlockInfo(userId)).thenReturn("Blocked for spam");

        Map<String, String> result = userManagementService.getBlockInfo(userId);

        assertEquals("true", result.get("isBlocked"));
        assertEquals("Blocked for spam", result.get("info"));
    }

    @Test
    @DisplayName("Should throw RuntimeException on error")
    void getBlockInfo_shouldThrowException() {
        Long userId = 1L;
        when(blockService.isUserBlocked(userId)).thenThrow(new RuntimeException("Redis error"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userManagementService.getBlockInfo(userId)
        );

        assertTrue(exception.getMessage().contains("Ошибка получения информации о блокировке"));
    }

    @Test
    @DisplayName("Should return user by username or email")
    void findUserByUsernameOrEmail_shouldReturnUser() {
        User user = new User();
        when(userRepository.getUserByUsernameOrEmail("test", "test")).thenReturn(Optional.of(user));

        User found = userManagementService.findUserByUsernameOrEmail("test");

        assertEquals(user, found);
    }

    @Test
    @DisplayName("Should return null when user not found")
    void findUserByUsernameOrEmail_shouldReturnNull() {
        when(userRepository.getUserByUsernameOrEmail("nope", "nope")).thenReturn(Optional.empty());

        User result = userManagementService.findUserByUsernameOrEmail("nope");

        assertNull(result);
    }
}