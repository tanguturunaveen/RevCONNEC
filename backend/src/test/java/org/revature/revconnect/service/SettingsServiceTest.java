package org.revature.revconnect.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.DuplicateResourceException;
import org.revature.revconnect.model.User;
import org.revature.revconnect.model.UserSettings;
import org.revature.revconnect.repository.UserRepository;
import org.revature.revconnect.repository.UserSettingsRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock private UserSettingsRepository userSettingsRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthService authService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SettingsService settingsService;

    @Test
    void getSecuritySettings_returnsUsernameAndEmail() {
        User me = user(1L, "u1");
        when(authService.getCurrentUser()).thenReturn(me);

        Map<String, Object> map = settingsService.getSecuritySettings();
        assertEquals("u1", map.get("username"));
        assertEquals("u1@test.com", map.get("email"));
    }

    @Test
    void getSettings_createsDefaultsWhenMissing() {
        User me = user(1L, "u1");
        when(authService.getCurrentUser()).thenReturn(me);
        when(userSettingsRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> settings = settingsService.getSettings();
        assertEquals("PUBLIC", settings.get("privacy"));
    }

    @Test
    void updateSettings_updatesPrivacyAndNotificationFlags() {
        User me = user(1L, "u1");
        UserSettings s = UserSettings.builder().user(me).build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(userSettingsRepository.findByUserId(1L)).thenReturn(Optional.of(s));

        Map<String, Object> payload = Map.of(
                "privacy", "private",
                "notifyLike", false,
                "notifyComment", false,
                "emailNotifications", true);

        Map<String, Object> res = settingsService.updateSettings(payload);

        assertEquals(Privacy.PRIVATE, me.getPrivacy());
        assertEquals(false, s.getNotifyLike());
        assertEquals(false, s.getNotifyComment());
        assertEquals("PRIVATE", res.get("privacy"));
        verify(userSettingsRepository).save(s);
        verify(userRepository).save(me);
    }

    @Test
    void updatePrivacySettings_invalid_throws() {
        User me = user(1L, "u1");
        when(authService.getCurrentUser()).thenReturn(me);

        assertThrows(BadRequestException.class,
                () -> settingsService.updatePrivacySettings(Map.of("privacy", "bad")));
    }

    @Test
    void addExternalLink_normalizesAndReturnsList() {
        User me = user(1L, "u1");
        me.setExternalLinks("");
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.save(me)).thenReturn(me);

        List<String> links = settingsService.addExternalLink("example.com");
        assertEquals(List.of("https://example.com"), links);
    }

    @Test
    void removeExternalLink_removesExisting() {
        User me = user(1L, "u1");
        me.setExternalLinks("https://a.com,https://b.com");
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.save(me)).thenReturn(me);

        List<String> links = settingsService.removeExternalLink("https://a.com");
        assertEquals(List.of("https://b.com"), links);
    }

    @Test
    void addExternalLink_invalid_throws() {
        assertThrows(BadRequestException.class, () -> settingsService.addExternalLink("   "));
    }

    @Test
    void changeEmail_duplicate_throws() {
        User me = user(1L, "u1");
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> settingsService.changeEmail("dup@test.com"));
    }

    @Test
    void changePassword_wrongCurrent_throws() {
        User me = user(1L, "u1");
        me.setPassword("enc-old");
        when(authService.getCurrentUser()).thenReturn(me);
        when(passwordEncoder.matches("bad", "enc-old")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> settingsService.changePassword("bad", "newPass"));
    }

    @Test
    void changePassword_success_updatesPassword() {
        User me = user(1L, "u1");
        me.setPassword("enc-old");
        when(authService.getCurrentUser()).thenReturn(me);
        when(passwordEncoder.matches("old", "enc-old")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("enc-new");
        when(userRepository.save(me)).thenReturn(me);

        settingsService.changePassword("old", "newPass");

        assertEquals("enc-new", me.getPassword());
    }

    @Test
    void deleteAccount_wrongPassword_throws() {
        User me = user(1L, "u1");
        me.setPassword("enc-old");
        when(authService.getCurrentUser()).thenReturn(me);
        when(passwordEncoder.matches("bad", "enc-old")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> settingsService.deleteAccount("bad"));
    }

    @Test
    void deleteAccount_correctPassword_noThrow() {
        User me = user(1L, "u1");
        me.setPassword("enc-old");
        when(authService.getCurrentUser()).thenReturn(me);
        when(passwordEncoder.matches("ok", "enc-old")).thenReturn(true);

        settingsService.deleteAccount("ok");
    }

    private User user(Long id, String username) {
        return User.builder().id(id).username(username).name(username)
                .email(username + "@test.com").password("x").privacy(Privacy.PUBLIC).build();
    }
}