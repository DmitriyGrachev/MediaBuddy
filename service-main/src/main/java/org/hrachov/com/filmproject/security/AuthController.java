package org.hrachov.com.filmproject.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hrachov.com.filmproject.model.PasswordResetToken;
import org.hrachov.com.filmproject.model.Role;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.dto.ResetPassDTO;
import org.hrachov.com.filmproject.repository.mongo.PasswordResetTokenRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.security.securityDTO.AuthRequest;
import org.hrachov.com.filmproject.security.securityDTO.AuthResponse;
import org.hrachov.com.filmproject.security.securityDTO.RegisterRequest;
import org.hrachov.com.filmproject.service.UserService;
import org.hrachov.com.filmproject.service.impl.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtils.generateToken(userDetails);

        // Проверяем, есть ли роль ROLE_ADMIN
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(new AuthResponse(token, isAdmin));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("User with this email already exists");
        }
        if(userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("User with this username already exists");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUsername(request.getUsername());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRegistrationDate(LocalDateTime.now());
        user.setRoles(Set.of(Role.ROLE_REGULAR));
        //TODO добавли провайдера локал после того как добавил Oauth
        user.setProvider("Local");
        user.setProviderId(UUID.randomUUID().toString());

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }
    @PostMapping("/send-email")
    public ResponseEntity<?> resetPassword(@Email @RequestParam String email) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("User with this email does not exist");
            }
            String uuid = UUID.randomUUID().toString();

            PasswordResetToken passwordResetToken = new PasswordResetToken();
            passwordResetToken.setToken(uuid);
            //Отсчет удаления начинаеться с данного момента
            passwordResetToken.setExpiryDate(Instant.now().plusSeconds(120));
            passwordResetToken.setUserId(user.getId());
            passwordResetTokenRepository.save(passwordResetToken);

            emailService.sendSimpleEmail(email, "Code for password reset", "Your code is : " + uuid);
            return ResponseEntity.ok("Code for password reset send successfully");
        }catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong");
        }
    }
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPassDTO resetPassDTO, BindingResult bindingResult){
        if(bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors()
                    .stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(errors);
        }
        try {
            userService.resetPassword(resetPassDTO);
            return ResponseEntity.ok("Password reset successfully");
        }catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong");
        }
    }

}
