package org.revature.revconnect.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        emailService = new EmailService(mailSender);
    }

    @Test
    void sendPasswordResetEmail_withValidInputs_doesNotThrow() {
        assertDoesNotThrow(() -> emailService.sendPasswordResetEmail("user@test.com", "123456"));
    }

    @Test
    void sendPasswordResetEmail_withNullInputs_doesNotThrow() {
        assertDoesNotThrow(() -> emailService.sendPasswordResetEmail(null, null));
    }
}
