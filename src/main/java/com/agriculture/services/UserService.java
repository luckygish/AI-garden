package com.agriculture.services;

import com.agriculture.models.PasswordResetToken;
import com.agriculture.models.User;
import com.agriculture.repository.PasswordResetTokenRepository;
import com.agriculture.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    public User registerUser(String email, String password, String region, String gardenType, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User already exists");
        }

        validatePasswordPolicy(password);

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRegion(region);
        user.setGardenType(gardenType);
        user.setName(name);

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public String createPasswordResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = generateSecureToken();
        PasswordResetToken resetToken = new PasswordResetToken(token, user, Instant.now().plus(30, ChronoUnit.MINUTES));
        passwordResetTokenRepository.save(resetToken);
        return token;
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));

        if (prt.isUsed() || prt.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Reset token expired or used");
        }

        validatePasswordPolicy(newPassword);

        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        prt.setUsed(true);
        passwordResetTokenRepository.save(prt);
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8 || password.length() > 12) {
            throw new RuntimeException("Password length must be between 8 and 12 characters");
        }
        // Базовые проверки:
        boolean hasLowerLetter = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpperLetter = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLowerLetter || !hasUpperLetter || !hasDigit) {
            throw new RuntimeException("Password must contain at least one uppercase letter, one lowercase letter and one digit");
        }
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}