package org.revature.revconnect.dto.request;

import org.revature.revconnect.enums.Privacy;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    private String bio;

    private String profilePicture;
    private String coverPhoto;
    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    private String website;

    private Privacy privacy;

    // Business/Creator fields
    @Size(max = 100, message = "Business name must not exceed 100 characters")
    private String businessName;

    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    @Size(max = 50, message = "Industry must not exceed 50 characters")
    private String industry;

    private String contactInfo;

    private String businessAddress;

    @Size(max = 100, message = "Business hours must not exceed 100 characters")
    private String businessHours;

    private String externalLinks;

    private String socialMediaLinks;
}