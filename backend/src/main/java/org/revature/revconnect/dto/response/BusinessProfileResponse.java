package org.revature.revconnect.dto.response;

import org.revature.revconnect.enums.BusinessCategory;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessProfileResponse {

    private Long id;
    private Long userId;
    private String username;
    private String businessName;
    private BusinessCategory category;
    private String description;
    private String websiteUrl;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String logoUrl;
    private String coverImageUrl;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
