package org.revature.revconnect.service;

import org.revature.revconnect.dto.request.ProfileUpdateRequest;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.UserResponse;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.mapper.UserMapper;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final UserMapper userMapper;

    public UserResponse getMyProfile() {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching profile for user: {}", currentUser.getUsername());
        return userMapper.toResponse(currentUser);
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        User currentUser = authService.getCurrentUser();

        // Check privacy settings
        if (user.getPrivacy() == Privacy.PRIVATE && !user.getId().equals(currentUser.getId())) {
            log.warn("Access denied to private profile: {}", userId);
            throw new UnauthorizedException("This profile is private");
        }

        return userMapper.toPublicResponse(user);
    }

    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        User currentUser = authService.getCurrentUser();

        // Check privacy settings
        if (user.getPrivacy() == Privacy.PRIVATE && !user.getId().equals(currentUser.getId())) {
            log.warn("Access denied to private profile: {}", username);
            throw new UnauthorizedException("This profile is private");
        }

        return userMapper.toPublicResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(ProfileUpdateRequest request) {
        User currentUser = authService.getCurrentUser();
        log.info("Updating profile for user: {}", currentUser.getUsername());

        // Update fields if provided
        if (request.getName() != null) {
            currentUser.setName(request.getName());
        }
        if (request.getBio() != null) {
            currentUser.setBio(request.getBio());
        }
        if (request.getProfilePicture() != null) {
            currentUser.setProfilePicture(request.getProfilePicture());
        }
        if (request.getCoverPhoto() != null) {
            currentUser.setCoverPhoto(request.getCoverPhoto());
        }
        if (request.getLocation() != null) {
            currentUser.setLocation(request.getLocation());
        }
        if (request.getWebsite() != null) {
            currentUser.setWebsite(request.getWebsite());
        }
        if (request.getPrivacy() != null) {
            currentUser.setPrivacy(request.getPrivacy());
        }

        // Business/Creator fields
        if (request.getBusinessName() != null) {
            currentUser.setBusinessName(request.getBusinessName());
        }
        if (request.getCategory() != null) {
            currentUser.setCategory(request.getCategory());
        }
        if (request.getIndustry() != null) {
            currentUser.setIndustry(request.getIndustry());
        }
        if (request.getContactInfo() != null) {
            currentUser.setContactInfo(request.getContactInfo());
        }
        if (request.getBusinessAddress() != null) {
            currentUser.setBusinessAddress(request.getBusinessAddress());
        }
        if (request.getBusinessHours() != null) {
            currentUser.setBusinessHours(request.getBusinessHours());
        }
        if (request.getExternalLinks() != null) {
            currentUser.setExternalLinks(request.getExternalLinks());
        }
        if (request.getSocialMediaLinks() != null) {
            currentUser.setSocialMediaLinks(request.getSocialMediaLinks());
        }

        User updatedUser = userRepository.save(currentUser);
        log.info("Profile updated successfully for user: {}", updatedUser.getUsername());

        return userMapper.toResponse(updatedUser);
    }

    public PagedResponse<UserResponse> searchUsers(String query, int page, int size) {
        log.info("Searching users with query: {}", query);

        Pageable pageable = PageRequest.of(page, size);
        Page<User> usersPage = userRepository.searchPublicUsers(query, pageable);

        return PagedResponse.<UserResponse>builder()
                .content(usersPage.getContent().stream()
                        .map(userMapper::toPublicResponse)
                        .toList())
                .pageNumber(usersPage.getNumber())
                .pageSize(usersPage.getSize())
                .totalElements(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages())
                .last(usersPage.isLast())
                .first(usersPage.isFirst())
                .build();
    }

    @Transactional
    public UserResponse updatePrivacy(Privacy privacy) {
        User currentUser = authService.getCurrentUser();
        log.info("Updating privacy for user {} to {}", currentUser.getUsername(), privacy);

        currentUser.setPrivacy(privacy);
        User updatedUser = userRepository.save(currentUser);

        return userMapper.toResponse(updatedUser);
    }

    public PagedResponse<UserResponse> getSuggestedUsers(int page, int size) {
        User currentUser = authService.getCurrentUser();
        log.info("Getting suggested users for: {}", currentUser.getUsername());

        Pageable pageable = PageRequest.of(page, size);
        // Get users that are not the current user, excluding blocked users
        Page<User> usersPage = userRepository.findSuggestedUsers(currentUser.getId(), pageable);

        return PagedResponse.<UserResponse>builder()
                .content(usersPage.getContent().stream()
                        .map(userMapper::toPublicResponse)
                        .toList())
                .pageNumber(usersPage.getNumber())
                .pageSize(usersPage.getSize())
                .totalElements(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages())
                .last(usersPage.isLast())
                .first(usersPage.isFirst())
                .build();
    }

    @Transactional
    public void blockUser(Long userId) {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getId().equals(userId)) {
            throw new org.revature.revconnect.exception.BadRequestException("Cannot block yourself");
        }

        User userToBlock = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        log.info("User {} blocking user {}", currentUser.getUsername(), userToBlock.getUsername());
        // In a real implementation, you would have a BlockedUser entity
        // For now, we just log the action
        log.info("User {} blocked successfully", userId);
    }

    @Transactional
    public void unblockUser(Long userId) {
        User currentUser = authService.getCurrentUser();
        User userToUnblock = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        log.info("User {} unblocking user {}", currentUser.getUsername(), userToUnblock.getUsername());
        // In a real implementation, you would remove from BlockedUser entity
        log.info("User {} unblocked successfully", userId);
    }

    public PagedResponse<UserResponse> getBlockedUsers(int page, int size) {
        User currentUser = authService.getCurrentUser();
        log.info("Getting blocked users for: {}", currentUser.getUsername());

        // In a real implementation, you would query the BlockedUser entity
        // For now, return an empty page
        return PagedResponse.<UserResponse>builder()
                .content(List.of())
                .pageNumber(page)
                .pageSize(size)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .first(true)
                .build();
    }

    @Transactional
    public void reportUser(Long userId, String reason) {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getId().equals(userId)) {
            throw new org.revature.revconnect.exception.BadRequestException("Cannot report yourself");
        }

        User userToReport = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        log.info("User {} reported user {} for reason: {}",
                currentUser.getUsername(), userToReport.getUsername(), reason);
        // In a real implementation, you would create a Report entity
        log.info("Report for user {} submitted successfully", userId);
    }

    public PagedResponse<UserResponse> getMutualConnections(Long userId, int page, int size) {
        User currentUser = authService.getCurrentUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        log.info("Getting mutual connections between {} and {}",
                currentUser.getUsername(), targetUser.getUsername());

        Pageable pageable = PageRequest.of(page, size);
        Page<User> mutualPage = userRepository.findMutualConnections(
                currentUser.getId(), userId, pageable);

        return PagedResponse.<UserResponse>builder()
                .content(mutualPage.getContent().stream()
                        .map(userMapper::toPublicResponse)
                        .toList())
                .pageNumber(mutualPage.getNumber())
                .pageSize(mutualPage.getSize())
                .totalElements(mutualPage.getTotalElements())
                .totalPages(mutualPage.getTotalPages())
                .last(mutualPage.isLast())
                .first(mutualPage.isFirst())
                .build();
    }
}
