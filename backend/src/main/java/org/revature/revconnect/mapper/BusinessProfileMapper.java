package org.revature.revconnect.mapper;

import org.revature.revconnect.dto.response.BusinessProfileResponse;
import org.revature.revconnect.model.BusinessProfile;
import org.springframework.stereotype.Component;

@Component
public class BusinessProfileMapper {

    public BusinessProfileResponse toResponse(BusinessProfile profile) {
        return BusinessProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .username(profile.getUser().getUsername())
                .businessName(profile.getBusinessName())
                .category(profile.getCategory())
                .description(profile.getDescription())
                .websiteUrl(profile.getWebsiteUrl())
                .contactEmail(profile.getContactEmail())
                .contactPhone(profile.getContactPhone())
                .address(profile.getAddress())
                .logoUrl(profile.getLogoUrl())
                .coverImageUrl(profile.getCoverImageUrl())
                .isVerified(profile.getIsVerified())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
