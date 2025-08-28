package org.hrachov.com.filmproject.service;

import org.hrachov.com.filmproject.exception.BlockUserException;
import org.hrachov.com.filmproject.service.impl.BlockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BlockServiceTest {
    private static final String BLOCK_PREFIX = "blocks:user:";

    @Mock
    private  RedisTemplate<String, Object> redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private BlockServiceImpl blockService;

    @BeforeEach
    void setUp() {
        blockService = new BlockServiceImpl(redisTemplate);

        //when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("Test block user Should work with until")
    void testBlockUserShouldWorkWithUntil() {
        LocalDateTime blockedAt = LocalDateTime.now();
        Long userId = 1L;
        String key = BLOCK_PREFIX  + userId;

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        blockService.blockUser(userId,"test", blockedAt.plusDays(1));

        verify(redisTemplate, times(1)).opsForHash();
        verify(hashOperations, times(1))
                .putAll(eq(key),any(Map.class));
    }
    @Test
    @DisplayName("Test block user Should throw block exceptin Because of LocalDate difference")
    void testBlockUserShouldThrowBlockExceptinBecauseOfLocalDateDifference() {
        LocalDateTime blockedAt = LocalDateTime.now();
        Long userId = 1L;
        String key = BLOCK_PREFIX  + userId;

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        BlockUserException blockUserException = assertThrows(BlockUserException.class,
                ()-> blockService.blockUser(userId,"test", blockedAt.minusDays(1))
        );

        verify(redisTemplate, times(1)).opsForHash();
        verify(redisTemplate, times(1)).delete(eq(key));
        assertEquals("User " + userId + " wasn't blocked. Exception thrown", blockUserException.getMessage());
    }
    @Test
    @DisplayName("Test permament User block")
    void testPermamentUserBlock() {
        Long userId = 1L;
        String key = BLOCK_PREFIX  + userId;

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        blockService.permanentBlockUser(userId,"Any reason");

        verify(redisTemplate, times(1)).opsForHash();
        verify(hashOperations, times(1)).putAll(eq(key),any(Map.class));

    }
    @Test
    @DisplayName("Test parmament Should throw if any issue")
    void testParmamentShouldThrowIfAnyIssue() {
        Long userId = 1L;
        String key = BLOCK_PREFIX  + userId;

        when(redisTemplate.opsForHash()).thenThrow(BlockUserException.class);

        BlockUserException blockUserException = assertThrows(
                BlockUserException.class,
                ()->blockService.permanentBlockUser(userId,"Any reason"));

        verify(redisTemplate, times(1)).delete(eq(key));
    }
    @Test
    @DisplayName("unblockUser должен разблокировать пользователя, если ключ существует")
    void unblockUser_shouldSucceedWhenKeyExists() {
        // Arrange
        Long userId = 1L;
        String key = BLOCK_PREFIX + userId;
        when(redisTemplate.delete(key)).thenReturn(true);

        // Act
        blockService.unblockUser(userId);

        // Assert
        verify(redisTemplate, times(1)).delete(key);
    }

    @Test
    @DisplayName("unblockUser должен выбросить исключение, если ключ не существует")
    void unblockUser_shouldThrowExceptionWhenKeyDoesNotExist() {
        // Arrange
        Long userId = 1L;
        String key = BLOCK_PREFIX + userId;
        when(redisTemplate.delete(key)).thenReturn(false);

        // Act & Assert
        BlockUserException exception = assertThrows(
                BlockUserException.class,
                () -> blockService.unblockUser(userId)
        );
        assertEquals("User either unblocked or not found in cache", exception.getMessage());
        verify(redisTemplate, times(1)).delete(key);
    }

    @Test
    @DisplayName("isUserBlocked должен вернуть true, если пользователь заблокирован")
    void isUserBlocked_shouldReturnTrueWhenUserIsBlocked() {
        // Arrange
        Long userId = 1L;
        String key = BLOCK_PREFIX + userId;
        when(redisTemplate.hasKey(key)).thenReturn(true);

        // Act
        boolean result = blockService.isUserBlocked(userId);

        // Assert
        assertTrue(result);
        verify(redisTemplate, times(1)).hasKey(key);
    }

    @Test
    @DisplayName("isUserBlocked должен вернуть false, если пользователь не заблокирован")
    void isUserBlocked_shouldReturnFalseWhenUserIsNotBlocked() {
        // Arrange
        Long userId = 1L;
        String key = BLOCK_PREFIX + userId;
        when(redisTemplate.hasKey(key)).thenReturn(false);

        // Act
        boolean result = blockService.isUserBlocked(userId);

        // Assert
        assertFalse(result);
        verify(redisTemplate, times(1)).hasKey(key);
    }

    @Test
    @DisplayName("getBlockInfo должен вернуть причину блокировки, если ключ существует")
    void getBlockInfo_shouldReturnReasonWhenKeyExists() {
        // Arrange
        Long userId = 1L;
        String key = BLOCK_PREFIX + userId;
        String reason = "Spamming";

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(key, "reason")).thenReturn(reason);

        // Act
        String result = blockService.getBlockInfo(userId);

        // Assert
        assertEquals(reason, result);
        verify(redisTemplate, times(1)).opsForHash();
        verify(hashOperations, times(1)).get(key, "reason");
    }

    @Test
    @DisplayName("getBlockInfo должен вернуть null, если причина отсутствует")
    void getBlockInfo_shouldReturnNullWhenReasonIsMissing() {
        // Arrange
        Long userId = 1L;
        String key = BLOCK_PREFIX + userId;
        when(hashOperations.get(key, "reason")).thenReturn(null);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        // Act
        String result = blockService.getBlockInfo(userId);

        // Assert
        assertNull(result);
        verify(redisTemplate, times(1)).opsForHash();
        verify(hashOperations, times(1)).get(key, "reason");
    }

    @Test
    @DisplayName("getFullBlockInfo должен вернуть полную информацию о блокировке, если ключ существует")
    void getFullBlockInfo_shouldReturnFullInfoWhenKeyExists() {
        // Arrange
        Long userId = 1L;
        String key = BLOCK_PREFIX + userId;
        Map<Object, Object> blockInfo = new HashMap<>();
        blockInfo.put("reason", "Spamming");
        blockInfo.put("blockedAt", "2025-07-21");

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.hasKey(key)).thenReturn(true);
        when(hashOperations.entries(key)).thenReturn(blockInfo);
        // Act
        Map<Object, Object> result = blockService.getFullBlockInfo(userId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Spamming", result.get("reason"));
        assertEquals("2025-07-21", result.get("blockedAt"));
        verify(redisTemplate, times(1)).hasKey(key);
        verify(redisTemplate, times(1)).opsForHash();
        verify(hashOperations, times(1)).entries(key);
    }

    @Test
    @DisplayName("getFullBlockInfo должен вернуть пустую карту, если ключ не существует")
    void getFullBlockInfo_shouldReturnEmptyMapWhenKeyDoesNotExist() {
        // Arrange
        Long userId = 1L;
        String key = BLOCK_PREFIX + userId;
        when(redisTemplate.hasKey(key)).thenReturn(false);

        // Act
        Map<Object, Object> result = blockService.getFullBlockInfo(userId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(redisTemplate, times(1)).hasKey(key);
        verify(redisTemplate, never()).opsForHash();
        verify(hashOperations, never()).entries(anyString());
    }

    @Test
    @DisplayName("getBlockTTL должен вернуть TTL, если ключ существует")
    void getBlockTTL_shouldReturnTTLWhenKeyExists() {
        // Arrange
        Long userId = 1L;
        String key = BLOCK_PREFIX + userId;
        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(3600L);

        // Act
        Long result = blockService.getBlockTTL(userId);

        // Assert
        assertEquals(3600L, result);
        verify(redisTemplate, times(1)).getExpire(key, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("getBlockTTL должен вернуть -2, если ключ не существует")
    void getBlockTTL_shouldReturnMinusTwoWhenKeyDoesNotExist() {
        // Arrange
        Long userId = 1L;
        String key = BLOCK_PREFIX + userId;
        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(-2L);

        // Act
        Long result = blockService.getBlockTTL(userId);

        // Assert
        assertEquals(-2L, result);
        verify(redisTemplate, times(1)).getExpire(key, TimeUnit.SECONDS);
    }

}

