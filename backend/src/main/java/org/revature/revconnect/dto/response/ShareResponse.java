package org.revature.revconnect.dto.response;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareResponse {

    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String profilePicture;
    private Long postId;
    private String comment;
    private LocalDateTime createdAt;


}