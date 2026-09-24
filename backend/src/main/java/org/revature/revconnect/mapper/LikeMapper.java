package org.revature.revconnect.mapper;
import org.revature.revconnect.dto.response.LikeResponse;
import org.revature.revconnect.model.Like;
import org.springframework.stereotype.Component;

@Component
public class LikeMapper {

    public LikeResponse toResponse(Like like) {
        return LikeResponse.builder()
                .id(like.getId())
                .userId(like.getUser().getId())
                .username(like.getUser().getUsername())
                .name(like.getUser().getName())
                .profilePicture(like.getUser().getProfilePicture())
                .createdAt(like.getCreatedAt())
                .build();
    }
}