package org.revature.revconnect.mapper;

import org.revature.revconnect.dto.response.UserResponse;
import org.revature.revconnect.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .userType(user.getUserType())
                .bio(user.getBio())
                .profilePicture(user.getProfilePicture())
                .coverPhoto(user.getCoverPhoto())
                .location(user.getLocation())
                .website(user.getWebsite())
                .privacy(user.getPrivacy())
                .isVerified(user.getIsVerified())
                .businessName(user.getBusinessName())
                .category(user.getCategory())
                .industry(user.getIndustry())
                .contactInfo(user.getContactInfo())
                .businessAddress(user.getBusinessAddress())
                .businessHours(user.getBusinessHours())
                .externalLinks(user.getExternalLinks())
                .socialMediaLinks(user.getSocialMediaLinks())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserResponse toPublicResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .userType(user.getUserType())
                .bio(user.getBio())
                .profilePicture(user.getProfilePicture())
                .coverPhoto(user.getCoverPhoto())
                .location(user.getLocation())
                .website(user.getWebsite())
                .privacy(user.getPrivacy())
                .isVerified(user.getIsVerified())
                .businessName(user.getBusinessName())
                .category(user.getCategory())
                .industry(user.getIndustry())
                .contactInfo(user.getContactInfo())
                .businessAddress(user.getBusinessAddress())
                .businessHours(user.getBusinessHours())
                .externalLinks(user.getExternalLinks())
                .socialMediaLinks(user.getSocialMediaLinks())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
