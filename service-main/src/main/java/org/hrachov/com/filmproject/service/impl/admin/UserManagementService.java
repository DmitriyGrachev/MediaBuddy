package org.hrachov.com.filmproject.service.impl.admin;

import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.exception.BlockUserException;
import org.hrachov.com.filmproject.exception.IlligalRoleException;
import org.hrachov.com.filmproject.exception.UserNotFoundException;
import org.hrachov.com.filmproject.model.Role;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.dto.BlockUserRequest;
import org.hrachov.com.filmproject.model.dto.UserDTO;
import org.hrachov.com.filmproject.model.dto.UserResponseDTO;
import org.hrachov.com.filmproject.repository.jpa.*;
import org.hrachov.com.filmproject.service.BlockService;
import org.hrachov.com.filmproject.service.FilmService;
import org.hrachov.com.filmproject.service.impl.ExternalApiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserManagementService {
    private final UserRepository userRepository;
    private final BlockService blockService;

    public List<UserResponseDTO> getAllUsersWithBlockInfo() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::createUserResponseDTO)
                .collect(Collectors.toList());
    }

    private UserResponseDTO createUserResponseDTO(User user) {
        boolean isBlocked = blockService.isUserBlocked(user.getId());
        String reason = null;
        String blockedUntilStr = null;
        String blockedAtStr = null;

        if (isBlocked) {
            Map<Object, Object> blockInfoMap = blockService.getFullBlockInfo(user.getId());
            if (blockInfoMap != null && !blockInfoMap.isEmpty()) {
                reason = (String) blockInfoMap.get("reason");
                Object blockedUntilObj = blockInfoMap.get("blockedUntil");
                blockedUntilStr = (blockedUntilObj == null || "null".equalsIgnoreCase(String.valueOf(blockedUntilObj)))
                        ? "Навсегда" : blockedUntilObj.toString();

                Object blockedAtObj = blockInfoMap.get("blockedAt");
                blockedAtStr = (blockedAtObj == null) ? null : blockedAtObj.toString();
            }
        }

        return new UserResponseDTO(user, isBlocked, reason, blockedUntilStr, blockedAtStr);
    }

    @Transactional
    public User deleteUserById(Long id) {
        User user = userRepository.getUserById(id);
        if (user == null) {
            throw new UserNotFoundException(id);
            //throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден");
        }
        userRepository.delete(user);
        return user;
    }

    @Transactional
    public User updateUser(UserDTO userDTO) {
        User user = userRepository.getUserById(userDTO.getId());
        //User user = userRepository.getReferenceById((long) userDTO.getId());
        if (user == null) {
            throw new UserNotFoundException(userDTO.getId());
        }
        updateUserFields(user, userDTO);
        return userRepository.save(user);
    }

    private void updateUserFields(User user, UserDTO userDTO) {
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setUsername(userDTO.getUsername());
    }

    public void blockUser(BlockUserRequest blockUserRequest) {
        try {
            if (blockUserRequest.getBlockedUntil() != null) {
                blockService.blockUser(
                        blockUserRequest.getUserId(),
                        blockUserRequest.getReason(),
                        blockUserRequest.getBlockedUntil()
                );
            } else {
                blockService.permanentBlockUser(
                        blockUserRequest.getUserId(),
                        blockUserRequest.getReason()
                );
            }
        } catch (Exception e) {
            throw new BlockUserException(blockUserRequest.getUserId());
        }
    }

    public void unblockUser(Long userId) {
        try {
            blockService.unblockUser(userId);
        } catch (Exception e) {
            throw new BlockUserException("User either unblocked or not found in cache");
        }
    }

    public Map<String, String> getBlockInfo(Long userId) {
        try {
            Map<String, String> result = new HashMap<>();
            result.put("isBlocked", String.valueOf(blockService.isUserBlocked(userId)));
            result.put("info", blockService.getBlockInfo(userId));
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка получения информации о блокировке", e);
        }
    }

    @Transactional
    public void updateUserRoles(Long userId, Set<String> newRoleStrings) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Set<Role> newRolesEnumSet = convertStringRolesToEnum(newRoleStrings);
        user.setRoles(newRolesEnumSet);
        userRepository.save(user);
    }

    private Set<Role> convertStringRolesToEnum(Set<String> roleStrings) {
        try {
            return roleStrings.stream()
                    .map(roleString -> Role.valueOf(roleString.trim().toUpperCase()))
                    .collect(Collectors.toSet());
        } catch (IllegalArgumentException e) {
            throw new IlligalRoleException();
        }
    }

    public User findUserByUsernameOrEmail(String query) {
        return userRepository.getUserByUsernameOrEmail(query, query).orElse(null);
    }
}
