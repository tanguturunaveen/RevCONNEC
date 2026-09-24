package org.revature.revconnect.dto.request;

import org.revature.revconnect.enums.BusinessCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessProfileRequest {

    @NotBlank(message = "Business name is required")
    @Size(max = 100, message = "Business name must be less than 100 characters")
    private String businessName;

    @NotNull(message = "Category is required")
    private BusinessCategory category;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    private String websiteUrl;

    @Email(message = "Invalid email format")
    private String contactEmail;

    private String contactPhone;

    private String address;

    private String logoUrl;

    private String coverImageUrl;
}
