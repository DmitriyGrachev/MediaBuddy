package org.hrachov.com.filmproject.service.impl;

import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.exception.BlockUserException;
import org.hrachov.com.filmproject.service.BlockService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ecs.model.BlockedException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
public class BlockServiceImpl implements BlockService {
    private static final String BLOCK_PREFIX = "blocks:user:";

    //Spring redis data special
    private final RedisTemplate<String, Object> redisTemplate;


    public void blockUser(Long userId, String reason , LocalDateTime blockedUntil) {
        LocalDateTime blockedAt = LocalDateTime.now();
        String key = BLOCK_PREFIX + userId;

            Map<String, Object> map = new HashMap<>();
            map.put("reason", reason);
            map.put("blockedAt", blockedAt.toString());
            map.put("blockedUntil", blockedUntil.toString());

            redisTemplate.opsForHash().putAll(key, map);

            if (blockedAt != null && blockedUntil.isAfter(blockedAt)) {
                Duration duration = Duration.between(blockedAt, blockedUntil);
                long ttlSeconds = duration.getSeconds();

                redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
            } else {
                redisTemplate.delete(key);
                throw new BlockUserException(userId);
            }

    }
    public void permanentBlockUser(Long userId, String reason) {
        String key = BLOCK_PREFIX + userId;
        try {
            LocalDateTime blockedAt = LocalDateTime.now();
            Map<String, Object> map = new HashMap<>();
            map.put("reason", reason);
            map.put("blockedAt", blockedAt.toString());
            map.put("blockedUntil", null);
            redisTemplate.opsForHash().putAll(key, map);
        }catch (BlockUserException e) {
            redisTemplate.delete(key);
            throw new BlockUserException(userId);
        }
    }

    public void unblockUser(Long userId) {
        String key = BLOCK_PREFIX + userId;
        if(!redisTemplate.delete(key)){
            throw new BlockUserException("User either unblocked or not found in cache");
        }
    }
    public boolean isUserBlocked(Long userId) {
        String key = BLOCK_PREFIX + userId;
        return redisTemplate.hasKey(key);
    }
    public String getBlockInfo(Long userId) {
        String key = BLOCK_PREFIX + userId;
        return (String) redisTemplate.opsForHash().get(key, "reason");
    }
    public Map<Object, Object> getFullBlockInfo(Long userId) {
        String key = BLOCK_PREFIX + userId;
        if (!redisTemplate.hasKey(key)) {
            return java.util.Collections.emptyMap(); // Возвращаем пустую карту, если нет информации
        }
        return redisTemplate.opsForHash().entries(key);
    }
    public Long getBlockTTL(Long userId) {
        String key = BLOCK_PREFIX + userId;
        // Возвращает оставшееся время жизни в секундах, или -1 если нет TTL, или -2 если ключ не существует
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
}
