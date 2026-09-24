package org.revature.revconnect.controller;
import org.revature.revconnect.dto.response.ApiResponse;
import org.revature.revconnect.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Settings", description = "User Settings APIs")
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    @Operation(summary = "Get all settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings() {
        log.info("Getting all settings");
        Map<String, Object> settings = settingsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping
    @Operation(summary = "Update all settings")
    public ResponseEntity<ApiResponse<Void>> updateSettings(@RequestBody Map<String, Object> settings) {
        log.info("Updating settings");
        settingsService.updateSettings(settings);
        return ResponseEntity.ok(ApiResponse.success("Settings updated", null));
    }

    @GetMapping("/privacy")
    @Operation(summary = "Get privacy settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPrivacySettings() {
        log.info("Getting privacy settings");
        Map<String, Object> settings = settingsService.getPrivacySettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping("/privacy")
    @Operation(summary = "Update privacy settings")
    public ResponseEntity<ApiResponse<Void>> updatePrivacySettings(@RequestBody Map<String, Object> settings) {
        log.info("Updating privacy settings");
        settingsService.updatePrivacySettings(settings);
        return ResponseEntity.ok(ApiResponse.success("Privacy settings updated", null));
    }

    @GetMapping("/notifications")
    @Operation(summary = "Get notification settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotificationSettings() {
        log.info("Getting notification settings");
        Map<String, Object> settings = settingsService.getNotificationSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping("/notifications")
    @Operation(summary = "Update notification settings")
    public ResponseEntity<ApiResponse<Void>> updateNotificationSettings(@RequestBody Map<String, Object> settings) {
        log.info("Updating notification settings");
        settingsService.updateNotificationSettings(settings);
        return ResponseEntity.ok(ApiResponse.success("Notification settings updated", null));
    }

    @GetMapping("/security")
    @Operation(summary = "Get security settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecuritySettings() {
        log.info("Getting security settings");
        Map<String, Object> settings = settingsService.getSecuritySettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping("/security")
    @Operation(summary = "Update security settings")
    public ResponseEntity<ApiResponse<Void>> updateSecuritySettings(@RequestBody Map<String, Object> settings) {
        log.info("Updating security settings");
        return ResponseEntity.ok(ApiResponse.success("Security settings updated", null));
    }

    @PostMapping("/password/change")
    @Operation(summary = "Change password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {
        log.info("Changing password");
        settingsService.changePassword(currentPassword, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password changed", null));
    }

    @PostMapping("/email/change")
    @Operation(summary = "Request email change")
    public ResponseEntity<ApiResponse<Void>> changeEmail(@RequestParam String newEmail) {
        log.info("Requesting email change");
        settingsService.changeEmail(newEmail);
        return ResponseEntity.ok(ApiResponse.success("Email changed", null));
    }

    @PostMapping("/email/verify")
    @Operation(summary = "Verify email change")
    public ResponseEntity<ApiResponse<Void>> verifyEmailChange(@RequestParam String token) {
        log.info("Verifying email change");
        return ResponseEntity.ok(ApiResponse.success("Email changed", null));
    }

    @GetMapping("/sessions")
    @Operation(summary = "Get active sessions")
    public ResponseEntity<ApiResponse<java.util.List<Map<String, Object>>>> getActiveSessions() {
        log.info("Getting active sessions");
        return ResponseEntity.ok(ApiResponse.success(java.util.List.of()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Revoke a session")
    public ResponseEntity<ApiResponse<Void>> revokeSession(@PathVariable String sessionId) {
        log.info("Revoking session: {}", sessionId);
        return ResponseEntity.ok(ApiResponse.success("Session revoked", null));
    }

    @DeleteMapping("/sessions")
    @Operation(summary = "Revoke all other sessions")
    public ResponseEntity<ApiResponse<Void>> revokeAllSessions() {
        log.info("Revoking all sessions");
        return ResponseEntity.ok(ApiResponse.success("All sessions revoked", null));
    }

    @PostMapping("/2fa/enable")
    @Operation(summary = "Enable two-factor authentication")
    public ResponseEntity<ApiResponse<Map<String, String>>> enable2FA() {
        log.info("Enabling 2FA");
        return ResponseEntity.ok(ApiResponse.success(Map.of("secret", "XXXX")));
    }

    @PostMapping("/2fa/verify")
    @Operation(summary = "Verify 2FA setup")
    public ResponseEntity<ApiResponse<Void>> verify2FA(@RequestParam String code) {
        log.info("Verifying 2FA");
        return ResponseEntity.ok(ApiResponse.success("2FA enabled", null));
    }

    @DeleteMapping("/2fa")
    @Operation(summary = "Disable two-factor authentication")
    public ResponseEntity<ApiResponse<Void>> disable2FA(@RequestParam String code) {
        log.info("Disabling 2FA");
        return ResponseEntity.ok(ApiResponse.success("2FA disabled", null));
    }

    @GetMapping("/account")
    @Operation(summary = "Get account settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccountSettings() {
        log.info("Getting account settings");
        Map<String, Object> settings = settingsService.getAccountSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @DeleteMapping("/account")
    @Operation(summary = "Delete account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@RequestParam String password) {
        log.info("Deleting account");
        settingsService.deleteAccount(password);
        return ResponseEntity.ok(ApiResponse.success("Account deleted", null));
    }

    @PostMapping("/account/deactivate")
    @Operation(summary = "Deactivate account temporarily")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount() {
        log.info("Deactivating account");
        settingsService.deactivateAccount();
        return ResponseEntity.ok(ApiResponse.success("Account deactivated", null));
    }

    @PostMapping("/account/reactivate")
    @Operation(summary = "Reactivate account")
    public ResponseEntity<ApiResponse<Void>> reactivateAccount() {
        log.info("Reactivating account");
        settingsService.reactivateAccount();
        return ResponseEntity.ok(ApiResponse.success("Account reactivated", null));
    }

    @GetMapping("/data/export")
    @Operation(summary = "Request data export")
    public ResponseEntity<ApiResponse<Void>> requestDataExport() {
        log.info("Requesting data export");
        return ResponseEntity.ok(ApiResponse.success("Data export requested", null));
    }

    @GetMapping("/account/external-links")
    @Operation(summary = "Get external links")
    public ResponseEntity<ApiResponse<List<String>>> getExternalLinks() {
        return ResponseEntity.ok(ApiResponse.success(settingsService.getExternalLinks()));
    }

    @PostMapping("/account/external-links")
    @Operation(summary = "Add external link")
    public ResponseEntity<ApiResponse<List<String>>> addExternalLink(@RequestParam String url) {
        List<String> links = settingsService.addExternalLink(url);
        return ResponseEntity.ok(ApiResponse.success("External link added", links));
    }

    @DeleteMapping("/account/external-links")
    @Operation(summary = "Remove external link")
    public ResponseEntity<ApiResponse<List<String>>> removeExternalLink(@RequestParam String url) {
        List<String> links = settingsService.removeExternalLink(url);
        return ResponseEntity.ok(ApiResponse.success("External link removed", links));
    }
}
