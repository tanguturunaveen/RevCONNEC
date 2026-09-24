package org.revature.revconnect.dto.response;

import org.revature.revconnect.model.Like;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeResponse {

    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String profilePicture;
    private LocalDateTime createdAt;

}