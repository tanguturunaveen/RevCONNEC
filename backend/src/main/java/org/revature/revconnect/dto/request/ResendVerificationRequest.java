package org.revature.revconnect.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendVerificationRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Format must be valid email")
    private String email;
}
