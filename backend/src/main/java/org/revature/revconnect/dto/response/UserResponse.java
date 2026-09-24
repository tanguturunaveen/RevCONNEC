package org.revature.revconnect.dto.response;

import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String name;
    private UserType userType;
    private String bio;
    private String profilePicture;
    private String coverPhoto;
    private String location;
    private String website;
    private Privacy privacy;
    private Boolean isVerified;

    // Business/Creator fields
    private String businessName;
    private String category;
    private String industry;
    private String contactInfo;
    private String businessAddress;
    private String businessHours;
    private String externalLinks;
    private String socialMediaLinks;

    private LocalDateTime createdAt;
}
