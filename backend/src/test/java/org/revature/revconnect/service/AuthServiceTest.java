package org.revature.revconnect.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.revature.revconnect.dto.request.ForgotPasswordRequest;
import org.revature.revconnect.dto.request.LoginRequest;
import org.revature.revconnect.dto.request.RegisterRequest;
import org.revature.revconnect.dto.request.ResetPasswordRequest;
import org.revature.revconnect.dto.response.AuthResponse;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.DuplicateResourceException;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.model.PasswordResetToken;
import org.revature.revconnect.model.User;
import org.revature.revconnect.model.UserSettings;
import org.revature.revconnect.repository.PasswordResetTokenRepository;
import org.revature.revconnect.repository.UserRepository;
import org.revature.revconnect.repository.UserSettingsRepository;
import org.revature.revconnect.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserSettingsRepository userSettingsRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void cleanupSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void register_whenUsernameExists_throwsDuplicateResource() {
        RegisterRequest req = registerRequest();
        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(req));
    }

    @Test
    void register_whenEmailExists_throwsDuplicateResource() {
        RegisterRequest req = registerRequest();
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@test.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(req));
    }

    @Test
    void register_success_savesUserAndSettings_andReturnsAuthResponse() {
        RegisterRequest req = registerRequest();
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("enc-pass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(101L);
            return u;
        });
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(req);

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals(101L, response.getUserId());
        assertEquals("john", response.getUsername());
        assertEquals("john@test.com", response.getEmail());
        assertEquals(UserType.PERSONAL, response.getUserType());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("enc-pass", userCaptor.getValue().getPassword());

        ArgumentCaptor<UserSettings> settingsCaptor = ArgumentCaptor.forClass(UserSettings.class);
        verify(userSettingsRepository).save(settingsCaptor.capture());
        assertNotNull(settingsCaptor.getValue().getUser());
        assertEquals(101L, settingsCaptor.getValue().getUser().getId());
    }

    @Test
    void login_success_returnsAuthResponseAndSetsContext() {
        LoginRequest req = LoginRequest.builder().usernameOrEmail("john").password("secret123").build();
        User principal = user(1L, "john");
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateToken(principal)).thenReturn("jwt-login");

        AuthResponse response = authService.login(req);

        assertEquals("jwt-login", response.getAccessToken());
        assertEquals(1L, response.getUserId());
        assertEquals("john", response.getUsername());
        assertEquals(auth, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void getCurrentUser_whenNoAuthentication_throwsBadRequest() {
        SecurityContextHolder.clearContext();
        assertThrows(BadRequestException.class, () -> authService.getCurrentUser());
    }

    @Test
    void getCurrentUser_whenNotAuthenticated_throwsBadRequest() {
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(BadRequestException.class, () -> authService.getCurrentUser());
    }

    @Test
    void getCurrentUser_success_returnsPrincipal() {
        User principal = user(1L, "john");
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(principal);
        SecurityContextHolder.getContext().setAuthentication(auth);

        User current = authService.getCurrentUser();
        assertEquals(1L, current.getId());
    }

    @Test
    void forgotPassword_whenUserNotFound_throwsResourceNotFound() {
        ForgotPasswordRequest req = ForgotPasswordRequest.builder().email("missing@test.com").build();
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.forgotPassword(req));
    }

    @Test
    void forgotPassword_whenExistingTokenPresent_deletesOldAndCreatesNew() {
        User u = user(5L, "john");
        ForgotPasswordRequest req = ForgotPasswordRequest.builder().email("john@test.com").build();
        PasswordResetToken oldToken = PasswordResetToken.builder()
                .id(10L).token("old").user(u).expiryDate(LocalDateTime.now().plusHours(1)).build();
        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(u));
        when(passwordResetTokenRepository.findByUser(u)).thenReturn(Optional.of(oldToken));

        authService.forgotPassword(req);

        verify(passwordResetTokenRepository).delete(oldToken);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("john@test.com"), any(String.class));
    }

    @Test
    void forgotPassword_whenNoExistingToken_createsAndEmails() {
        User u = user(5L, "john");
        ForgotPasswordRequest req = ForgotPasswordRequest.builder().email("john@test.com").build();
        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(u));
        when(passwordResetTokenRepository.findByUser(u)).thenReturn(Optional.empty());

        authService.forgotPassword(req);

        verify(passwordResetTokenRepository, never()).delete(any(PasswordResetToken.class));
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("john@test.com"), any(String.class));
    }

    @Test
    void resetPassword_whenTokenNotFound_throwsBadRequest() {
        ResetPasswordRequest req = ResetPasswordRequest.builder().token("bad-token").newPassword("newPass123").build();
        when(passwordResetTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> authService.resetPassword(req));
    }

    @Test
    void resetPassword_whenTokenExpired_deletesTokenAndThrows() {
        User u = user(1L, "john");
        PasswordResetToken expired = PasswordResetToken.builder()
                .token("expired")
                .user(u)
                .expiryDate(LocalDateTime.now().minusMinutes(1))
                .build();
        ResetPasswordRequest req = ResetPasswordRequest.builder().token("expired").newPassword("newPass123").build();
        when(passwordResetTokenRepository.findByToken("expired")).thenReturn(Optional.of(expired));

        assertThrows(BadRequestException.class, () -> authService.resetPassword(req));
        verify(passwordResetTokenRepository).delete(expired);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_success_updatesPasswordAndDeletesToken() {
        User u = user(1L, "john");
        PasswordResetToken token = PasswordResetToken.builder()
                .token("ok")
                .user(u)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .build();
        ResetPasswordRequest req = ResetPasswordRequest.builder().token("ok").newPassword("newPass123").build();
        when(passwordResetTokenRepository.findByToken("ok")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPass123")).thenReturn("enc-new");

        authService.resetPassword(req);

        assertEquals("enc-new", u.getPassword());
        verify(userRepository).save(u);
        verify(passwordResetTokenRepository).delete(token);
    }

    private RegisterRequest registerRequest() {
        return RegisterRequest.builder()
                .username("john")
                .email("john@test.com")
                .password("secret123")
                .name("John Doe")
                .userType(UserType.PERSONAL)
                .build();
    }

    private User user(Long id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .email(username + "@test.com")
                .name(username)
                .password("pwd")
                .privacy(Privacy.PUBLIC)
                .userType(UserType.PERSONAL)
                .build();
    }
}
