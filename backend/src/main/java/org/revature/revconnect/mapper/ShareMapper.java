package org.revature.revconnect.mapper;

import org.revature.revconnect.dto.response.ShareResponse;
import org.revature.revconnect.model.Share;
import org.springframework.stereotype.Component;

@Component
public class ShareMapper {

    public ShareResponse toResponse(Share share) {
        return ShareResponse.builder()
                .id(share.getId())
                .userId(share.getUser().getId())
                .username(share.getUser().getUsername())
                .name(share.getUser().getName())
                .profilePicture(share.getUser().getProfilePicture())
                .postId(share.getPost().getId())
                .comment(share.getComment())
                .createdAt(share.getCreatedAt())
                .build();
    }
}
