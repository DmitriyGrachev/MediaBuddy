package org.hrachov.com.filmproject.service;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface BlockService {
     void blockUser(Long userId, String reason , LocalDateTime blockedUntil);
     void permanentBlockUser(Long userId, String reason);
     void unblockUser(Long userId);
     boolean isUserBlocked(Long userId);
     String getBlockInfo(Long userId);
     Map<Object, Object> getFullBlockInfo(Long userId);
     Long getBlockTTL(Long userId);
}
