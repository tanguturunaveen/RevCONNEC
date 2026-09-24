package org.revature.revconnect.service;

import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.DuplicateResourceException;
import org.revature.revconnect.model.User;
import org.revature.revconnect.model.UserSettings;
import org.revature.revconnect.repository.UserRepository;
import org.revature.revconnect.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Map<String, Object> getSettings() {
        User currentUser = authService.getCurrentUser();
        UserSettings settings = getOrCreateSettings(currentUser);
        return buildSettingsMap(currentUser, settings);
    }

    @Transactional
    public Map<String, Object> updateSettings(Map<String, Object> settingsMap) {
        User currentUser = authService.getCurrentUser();
        UserSettings settings = getOrCreateSettings(currentUser);

        applyNotificationSettings(settings, settingsMap);
        applyPrivacySetting(currentUser, settingsMap);

        userSettingsRepository.save(settings);
        userRepository.save(currentUser);
        return buildSettingsMap(currentUser, settings);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPrivacySettings() {
        User currentUser = authService.getCurrentUser();
        Map<String, Object> map = new HashMap<>();
        map.put("privacy", currentUser.getPrivacy().name());
        return map;
    }

    @Transactional
    public void updatePrivacySettings(Map<String, Object> settingsMap) {
        User currentUser = authService.getCurrentUser();
        applyPrivacySetting(currentUser, settingsMap);
        userRepository.save(currentUser);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationSettings() {
        User currentUser = authService.getCurrentUser();
        UserSettings settings = getOrCreateSettings(currentUser);
        return buildNotificationSettingsMap(settings);
    }

    @Transactional
    public void updateNotificationSettings(Map<String, Object> settingsMap) {
        User currentUser = authService.getCurrentUser();
        UserSettings settings = getOrCreateSettings(currentUser);
        applyNotificationSettings(settings, settingsMap);
        userSettingsRepository.save(settings);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSecuritySettings() {
        User currentUser = authService.getCurrentUser();
        Map<String, Object> map = new HashMap<>();
        map.put("email", currentUser.getEmail());
        map.put("username", currentUser.getUsername());
        return map;
    }

    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        User currentUser = authService.getCurrentUser();
        if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        currentUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(currentUser);
    }

    @Transactional
    public void changeEmail(String newEmail) {
        User currentUser = authService.getCurrentUser();
        if (userRepository.existsByEmail(newEmail)) {
            throw new DuplicateResourceException("User", "email", newEmail);
        }
        currentUser.setEmail(newEmail);
        userRepository.save(currentUser);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAccountSettings() {
        User currentUser = authService.getCurrentUser();
        Map<String, Object> map = new HashMap<>();
        map.put("id", currentUser.getId());
        map.put("username", currentUser.getUsername());
        map.put("email", currentUser.getEmail());
        map.put("name", currentUser.getName());
        map.put("userType", currentUser.getUserType().name());
        map.put("isVerified", currentUser.getIsVerified());
        return map;
    }

    @Transactional
    public void deleteAccount(String password) {
        User currentUser = authService.getCurrentUser();
        if (!passwordEncoder.matches(password, currentUser.getPassword())) {
            throw new BadRequestException("Password is incorrect");
        }
        log.info("Deleting account for user: {}", currentUser.getUsername());

        // Due to CascadeType.ALL on User entity relationships,
        // this will delete all dependent records automatically.
        userRepository.delete(currentUser);
        log.info("Account for user {} deleted successfully", currentUser.getUsername());
    }

    @Transactional
    public void deactivateAccount() {
        User currentUser = authService.getCurrentUser();
        currentUser.setIsActive(false);
        userRepository.save(currentUser);
        log.info("Account deactivated for user: {}", currentUser.getUsername());
    }

    @Transactional
    public void reactivateAccount() {
        User currentUser = authService.getCurrentUser();
        currentUser.setIsActive(true);
        userRepository.save(currentUser);
        log.info("Account reactivated for user: {}", currentUser.getUsername());
    }

    @Transactional(readOnly = true)
    public List<String> getExternalLinks() {
        User currentUser = authService.getCurrentUser();
        return parseExternalLinks(currentUser.getExternalLinks());
    }

    @Transactional
    public List<String> addExternalLink(String url) {
        String normalized = normalizeUrl(url);
        if (normalized == null) {
            throw new BadRequestException("Invalid URL");
        }
        User currentUser = authService.getCurrentUser();
        Set<String> links = new LinkedHashSet<>(parseExternalLinks(currentUser.getExternalLinks()));
        links.add(normalized);
        currentUser.setExternalLinks(String.join(",", links));
        userRepository.save(currentUser);
        return new ArrayList<>(links);
    }

    @Transactional
    public List<String> removeExternalLink(String url) {
        String normalized = normalizeUrl(url);
        if (normalized == null) {
            throw new BadRequestException("Invalid URL");
        }
        User currentUser = authService.getCurrentUser();
        Set<String> links = new LinkedHashSet<>(parseExternalLinks(currentUser.getExternalLinks()));
        links.remove(normalized);
        currentUser.setExternalLinks(String.join(",", links));
        userRepository.save(currentUser);
        return new ArrayList<>(links);
    }

    private void applyPrivacySetting(User currentUser, Map<String, Object> settingsMap) {
        if (settingsMap == null) {
            return;
        }
        Object privacyValue = settingsMap.get("privacy");
        if (privacyValue == null) {
            return;
        }
        try {
            Privacy privacy = Privacy.valueOf(privacyValue.toString().trim().toUpperCase());
            currentUser.setPrivacy(privacy);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid privacy value: " + privacyValue);
        }
    }

    private void applyNotificationSettings(UserSettings settings, Map<String, Object> settingsMap) {
        if (settingsMap == null) {
            return;
        }
        Object notificationsValue = settingsMap.get("notifications");
        if (notificationsValue instanceof Map) {
            applyNotificationSettings(settings, (Map<String, Object>) notificationsValue);
            return;
        }

        applyBoolean(settingsMap, "notifyConnectionRequest", settings::setNotifyConnectionRequest);
        applyBoolean(settingsMap, "notifyConnectionAccepted", settings::setNotifyConnectionAccepted);
        applyBoolean(settingsMap, "notifyNewFollower", settings::setNotifyNewFollower);
        applyBoolean(settingsMap, "notifyLike", settings::setNotifyLike);
        applyBoolean(settingsMap, "notifyComment", settings::setNotifyComment);
        applyBoolean(settingsMap, "notifyShare", settings::setNotifyShare);
        applyBoolean(settingsMap, "emailNotifications", settings::setEmailNotifications);
    }

    private void applyBoolean(Map<String, Object> map, String key, java.util.function.Consumer<Boolean> setter) {
        if (!map.containsKey(key)) {
            return;
        }
        Boolean value = parseBoolean(map.get(key));
        if (value != null) {
            setter.accept(value);
        }
    }

    private Boolean parseBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean(((String) value).trim());
        }
        return null;
    }

    private UserSettings getOrCreateSettings(User user) {
        return userSettingsRepository.findByUserId(user.getId())
                .orElseGet(() -> userSettingsRepository.save(UserSettings.builder().user(user).build()));
    }

    private Map<String, Object> buildSettingsMap(User user, UserSettings settings) {
        Map<String, Object> map = new HashMap<>();
        map.put("privacy", user.getPrivacy().name());
        map.put("notifications", buildNotificationSettingsMap(settings));
        return map;
    }

    private Map<String, Object> buildNotificationSettingsMap(UserSettings settings) {
        Map<String, Object> map = new HashMap<>();
        map.put("notifyConnectionRequest", settings.getNotifyConnectionRequest());
        map.put("notifyConnectionAccepted", settings.getNotifyConnectionAccepted());
        map.put("notifyNewFollower", settings.getNotifyNewFollower());
        map.put("notifyLike", settings.getNotifyLike());
        map.put("notifyComment", settings.getNotifyComment());
        map.put("notifyShare", settings.getNotifyShare());
        map.put("emailNotifications", settings.getEmailNotifications());
        return map;
    }

    private List<String> parseExternalLinks(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String cleaned = raw.trim();
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).replace("\"", "");
        }
        String[] parts = cleaned.split("[,\\n]");
        List<String> links = new ArrayList<>();
        for (String part : parts) {
            String v = part.trim();
            if (!v.isBlank()) {
                links.add(v);
            }
        }
        return links;
    }

    private String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }
        String normalized = url.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (!(normalized.startsWith("http://") || normalized.startsWith("https://"))) {
            normalized = "https://" + normalized;
        }
        return normalized;
    }
}
