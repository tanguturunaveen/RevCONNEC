package org.revature.revconnect.service;

import org.revature.revconnect.dto.request.ForgotPasswordRequest;
import org.revature.revconnect.dto.request.LoginRequest;
import org.revature.revconnect.dto.request.RegisterRequest;
import org.revature.revconnect.dto.request.ResetPasswordRequest;
import org.revature.revconnect.dto.request.VerifyEmailRequest;
import org.revature.revconnect.dto.request.ResendVerificationRequest;
import org.revature.revconnect.dto.response.AuthResponse;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.DuplicateResourceException;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.model.PasswordResetToken;
import org.revature.revconnect.model.User;
import org.revature.revconnect.model.UserSettings;
import org.revature.revconnect.repository.PasswordResetTokenRepository;
import org.revature.revconnect.repository.UserRepository;
import org.revature.revconnect.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.revature.revconnect.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    private static final int TOKEN_EXPIRY_HOURS = 24;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with username: {}", request.getUsername());

        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .userType(request.getUserType())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        // Create default user settings
        UserSettings settings = UserSettings.builder()
                .user(savedUser)
                .build();
        userSettingsRepository.save(settings);

        // Generate 6-digit OTP for Email Verification
        String otp = String.format("%06d", (int) (Math.random() * 1000000));
        PasswordResetToken verificationToken = PasswordResetToken.builder()
                .token(otp)
                .user(savedUser)
                .expiryDate(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS))
                .build();
        passwordResetTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(savedUser.getEmail(), otp);

        return AuthResponse.builder()
                .accessToken("") // no token until verified
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .userType(savedUser.getUserType())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for user: {}", request.getUsernameOrEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = (User) authentication.getPrincipal();
        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            throw new BadRequestException("Please verify your email before logging in.");
        }

        // Reactivate account if it was deactivated
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            user.setIsActive(true);
            userRepository.save(user);
            log.info("Account reactivated on login for user: {}", user.getUsername());
        }

        String token = jwtTokenProvider.generateToken(user);

        log.info("User logged in successfully: {}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .userType(user.getUserType())
                .build();
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("No authenticated user found");
        }
        return (User) authentication.getPrincipal();
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        log.info("Processing email verification for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (user.getIsVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        PasswordResetToken token = passwordResetTokenRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("No verification token found"));

        if (!token.getToken().equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP");
        }

        if (token.isExpired()) {
            passwordResetTokenRepository.delete(token);
            throw new BadRequestException("Verification OTP has expired. Please request a new one.");
        }

        user.setIsVerified(true);
        userRepository.save(user);
        passwordResetTokenRepository.delete(token);

        String jwt = jwtTokenProvider.generateToken(user);

        log.info("Email verification successful for user: {}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .userType(user.getUserType())
                .build();
    }

    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        log.info("Processing resend verification request for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (user.getIsVerified()) {
            throw new BadRequestException("Email is already verified. Please log in.");
        }

        passwordResetTokenRepository.findByUser(user).ifPresent(passwordResetTokenRepository::delete);
        passwordResetTokenRepository.flush();

        String otp = String.format("%06d", (int) (Math.random() * 1000000));
        PasswordResetToken newToken = PasswordResetToken.builder()
                .token(otp)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS))
                .build();
        passwordResetTokenRepository.save(newToken);

        emailService.sendVerificationEmail(user.getEmail(), otp);
        log.info("Verification email resent for user: {}", user.getUsername());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Processing forgot password request for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        log.info("User found for email: {}", user.getUsername());

        // Generate new 6-digit OTP
        String otp = String.format("%06d", (int) (Math.random() * 1000000));
        log.info("Generated 6-digit OTP: {}", otp);

        // Find existing token and delete it to match test expectation and ensure fresh
        // OTP
        passwordResetTokenRepository.findByUser(user).ifPresent(token -> {
            passwordResetTokenRepository.delete(token);
            passwordResetTokenRepository.flush(); // Ensure deletion is flushed
        });

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(otp);
        resetToken.setExpiryDate(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS));

        try {
            passwordResetTokenRepository.save(resetToken);
            log.info("Password reset token (OTP) saved successfully for user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Error while saving reset token: {}", e.getMessage());
            // If it still fails due to uniqueness, try one more time by deleting first and
            // flushing
            try {
                log.info("Fall-back: Deleting and flushing before save...");
                passwordResetTokenRepository.deleteByUser(user);
                passwordResetTokenRepository.flush();

                PasswordResetToken newToken = PasswordResetToken.builder()
                        .token(otp)
                        .user(user)
                        .expiryDate(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS))
                        .build();
                passwordResetTokenRepository.save(newToken);
                log.info("Password reset token saved successfully after fall-back.");
            } catch (Exception ex) {
                log.error("Critical error in fall-back: {}", ex.getMessage());
                throw new BadRequestException("Failed to initiate password reset. Please try again later.");
            }
        }

        // Send email (mocked in dev)
        log.info("Calling email service to send OTP...");
        emailService.sendPasswordResetEmail(user.getEmail(), otp);
        log.info("Forgot password process completed for email: {}", request.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset for token");

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BadRequestException("Reset token has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Delete the used token
        passwordResetTokenRepository.delete(resetToken);

        log.info("Password reset successful for user: {}", user.getUsername());
    }
}
