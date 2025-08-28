package org.hrachov.com.filmproject.security;

import lombok.RequiredArgsConstructor;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.service.BlockService;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;
    private final BlockService blockService;

    @Override
    @Transactional(readOnly = true) // Keeps the session open for lazy loading
    public UserDetails loadUserByUsername(String username){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Проверка на блокировку
        if (blockService.isUserBlocked(user.getId())) {
            String reason = blockService.getBlockInfo(user.getId());
            String blockMessage = "User account is blocked.";
            if (reason != null && !reason.trim().isEmpty()) {
                blockMessage += " Reason: " + reason;
            }
            throw new LockedException(blockMessage); // Выбрасываем исключение, если заблокирован
        }
        return new UserDetailsImpl(user);
    }
}
