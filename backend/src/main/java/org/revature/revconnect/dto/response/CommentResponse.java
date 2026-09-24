package org.revature.revconnect.dto.response;

import org.revature.revconnect.model.Comment;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {

    private Long id;
    private String content;
    private Long userId;
    private String username;
    private String name;
    private String profilePicture;
    private Long postId;
    private Long parentId;
    private Integer likeCount;
    private Integer replyCount;
    private Boolean isLikedByCurrentUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}