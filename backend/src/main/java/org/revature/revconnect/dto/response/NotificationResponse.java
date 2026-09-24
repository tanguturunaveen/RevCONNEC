package org.revature.revconnect.dto.response;

import org.revature.revconnect.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String message;
    private Long actorId;
    private String actorUsername;
    private String actorName;
    private String actorProfilePicture;
    private Long referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
