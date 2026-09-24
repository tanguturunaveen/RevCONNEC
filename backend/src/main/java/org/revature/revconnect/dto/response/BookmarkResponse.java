package org.revature.revconnect.dto.response;

import lombok.*;
import org.revature.revconnect.dto.response.PostResponse;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookmarkResponse {
    private Long id;
    private PostResponse post;
    private LocalDateTime bookmarkedAt;
}
