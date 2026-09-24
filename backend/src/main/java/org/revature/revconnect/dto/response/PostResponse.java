package org.revature.revconnect.dto.response;

import org.revature.revconnect.enums.PostType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {

    private Long id;
    private String content;
    private PostType postType;
    private List<String> mediaUrls;
    private Boolean pinned;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Author info
    private Long authorId;
    private String authorUsername;
    private String authorName;
    private String authorProfilePicture;

    // Business/Creator fields
    private String ctaLabel;
    private String ctaUrl;
    private Boolean isPromotional;
    private String partnerName;
    private List<String> productTags;
    private Boolean isLikedByCurrentUser;
    private PostResponse originalPost;
}
