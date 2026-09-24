package org.revature.revconnect.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private static final String FROM_EMAIL = "ganeshchinamana39@gmail.com";
    private static final String FROM_NAME = "RevConnect";

    /**
     * Send a password reset email using Gmail SMTP with HTML content.
     *
     * @param toEmail recipient email address
     * @param otp     the 6-digit OTP
     */
    public void sendPasswordResetEmail(String toEmail, String otp) {
        log.info("Attempting to send real OTP email to: {}", toEmail);

        String subject = "Your RevConnect Password Reset Code";
        String htmlBody = buildHtmlEmail(
                "Password Reset Request",
                "You have requested to reset your password. Use the code below to proceed:",
                otp,
                "This code is valid for 24 hours. If you did not request this, please ignore this email."
        );

        try {
            sendHtmlEmail(toEmail, subject, htmlBody);
            log.info("OTP email successfully sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("CRITICAL ERROR: Failed to send real email to {}: {}", toEmail, e.getMessage());
            log.info("========================================");
            log.info("FALLBACK OTP (Console): {}", otp);
            log.info("========================================");
        }
    }

    /**
     * Send an account verification email using Gmail SMTP with HTML content.
     *
     * @param toEmail recipient email address
     * @param otp     the 6-digit OTP
     */
    public void sendVerificationEmail(String toEmail, String otp) {
        log.info("Attempting to send Verification OTP email to: {}", toEmail);

        String subject = "Verify Your RevConnect Account";
        String htmlBody = buildHtmlEmail(
                "Welcome to RevConnect!",
                "To complete your registration, please enter the verification code below:",
                otp,
                "This code is valid for 24 hours. If you did not create an account, please ignore this email."
        );

        try {
            sendHtmlEmail(toEmail, subject, htmlBody);
            log.info("Verification OTP email successfully sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("CRITICAL ERROR: Failed to send verify email to {}: {}", toEmail, e.getMessage());
            log.info("========================================");
            log.info("FALLBACK VERIFY OTP (Console): {}", otp);
            log.info("========================================");
        }
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlBody) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setFrom(FROM_EMAIL, FROM_NAME);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        helper.setReplyTo(FROM_EMAIL);
        mimeMessage.addHeader("X-Priority", "1");
        mimeMessage.addHeader("List-Unsubscribe", "<mailto:" + FROM_EMAIL + "?subject=unsubscribe>");
        mailSender.send(mimeMessage);
    }

    private String buildHtmlEmail(String heading, String message, String otp, String footer) {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'></head>" +
                "<body style='margin:0;padding:0;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif;background-color:#0f0a1a;'>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background:#0f0a1a;padding:40px 0;'>" +
                "<tr><td align='center'>" +

                // Logo & Brand Header
                "<table width='520' cellpadding='0' cellspacing='0' style='margin-bottom:0;'>" +
                "<tr><td style='text-align:center;padding:24px 0 20px;'>" +
                "<div style='display:inline-block;background:linear-gradient(135deg,#8b5cf6,#6366f1,#3b82f6);width:56px;height:56px;border-radius:16px;line-height:56px;text-align:center;box-shadow:0 8px 32px rgba(139,92,246,0.4);'>" +
                "<span style='font-size:28px;font-weight:900;color:#ffffff;font-family:Arial,sans-serif;'>R</span>" +
                "</div>" +
                "<p style='margin:12px 0 0;font-size:20px;font-weight:800;letter-spacing:-0.5px;'>" +
                "<span style='color:#8b5cf6;'>Rev</span><span style='color:#ffffff;'>Connect</span></p>" +
                "</td></tr></table>" +

                // Main Card
                "<table width='520' cellpadding='0' cellspacing='0' style='background:#1a1425;border-radius:20px;border:1px solid rgba(139,92,246,0.15);box-shadow:0 20px 60px rgba(0,0,0,0.5),0 0 40px rgba(139,92,246,0.08);overflow:hidden;'>" +

                // Gradient Accent Bar
                "<tr><td style='background:linear-gradient(135deg,#8b5cf6,#6366f1,#3b82f6);height:4px;font-size:0;line-height:0;'>&nbsp;</td></tr>" +

                // Heading Section
                "<tr><td style='padding:36px 40px 0;text-align:center;'>" +
                "<h1 style='margin:0 0 8px;color:#ffffff;font-size:24px;font-weight:800;letter-spacing:-0.5px;'>" + heading + "</h1>" +
                "<div style='width:48px;height:3px;background:linear-gradient(90deg,#8b5cf6,#3b82f6);margin:0 auto;border-radius:2px;'></div>" +
                "</td></tr>" +

                // Body Content
                "<tr><td style='padding:28px 40px 12px;'>" +
                "<p style='margin:0 0 24px;color:#c4b5d4;font-size:15px;line-height:1.7;text-align:center;'>" + message + "</p>" +

                // OTP Code Box
                "<div style='text-align:center;margin:0 0 28px;'>" +
                "<div style='display:inline-block;background:linear-gradient(135deg,rgba(139,92,246,0.12),rgba(99,102,241,0.08));border:1px solid rgba(139,92,246,0.25);border-radius:16px;padding:20px 48px;'>" +
                "<span style='font-size:36px;font-weight:900;letter-spacing:10px;color:#a78bfa;font-family:monospace;'>" + otp + "</span>" +
                "</div></div>" +

                // Footer Note
                "<div style='background:rgba(139,92,246,0.06);border-radius:12px;border:1px solid rgba(139,92,246,0.1);padding:16px 20px;margin-bottom:8px;'>" +
                "<p style='margin:0;color:#9586a8;font-size:13px;line-height:1.6;text-align:center;'>" +
                "&#128274; " + footer + "</p>" +
                "</div>" +
                "</td></tr>" +

                // Divider
                "<tr><td style='padding:0 40px;'>" +
                "<div style='height:1px;background:linear-gradient(90deg,transparent,rgba(139,92,246,0.2),transparent);'></div>" +
                "</td></tr>" +

                // Brand Footer
                "<tr><td style='padding:24px 40px 32px;text-align:center;'>" +
                "<p style='margin:0 0 8px;color:#6b5b7b;font-size:12px;font-weight:600;letter-spacing:0.5px;text-transform:uppercase;'>Powered by RevConnect</p>" +
                "<p style='margin:0 0 16px;color:#4a3d5c;font-size:11px;line-height:1.5;'>Professional Networking &bull; Business Growth &bull; Creator Platform</p>" +
                "<div style='display:inline-block;'>" +
                "<a href='#' style='display:inline-block;width:32px;height:32px;background:rgba(139,92,246,0.1);border:1px solid rgba(139,92,246,0.15);border-radius:8px;line-height:32px;text-align:center;text-decoration:none;margin:0 4px;color:#8b5cf6;font-size:14px;'>&#127760;</a>" +
                "<a href='#' style='display:inline-block;width:32px;height:32px;background:rgba(139,92,246,0.1);border:1px solid rgba(139,92,246,0.15);border-radius:8px;line-height:32px;text-align:center;text-decoration:none;margin:0 4px;color:#8b5cf6;font-size:14px;'>&#9993;</a>" +
                "<a href='#' style='display:inline-block;width:32px;height:32px;background:rgba(139,92,246,0.1);border:1px solid rgba(139,92,246,0.15);border-radius:8px;line-height:32px;text-align:center;text-decoration:none;margin:0 4px;color:#8b5cf6;font-size:14px;'>&#128279;</a>" +
                "</div>" +
                "</td></tr>" +

                "</table>" +

                // Bottom Copyright
                "<table width='520' cellpadding='0' cellspacing='0'>" +
                "<tr><td style='text-align:center;padding:20px 0;'>" +
                "<p style='margin:0;color:#3d2e50;font-size:11px;'>&copy; 2025 RevConnect. All rights reserved.</p>" +
                "</td></tr></table>" +

                "</td></tr></table></body></html>";
    }
}
