package org.revature.revconnect.mapper;

import org.revature.revconnect.dto.response.NotificationResponse;
import org.revature.revconnect.model.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(notification.getMessage())
                .actorId(notification.getActor().getId())
                .actorUsername(notification.getActor().getUsername())
                .actorName(notification.getActor().getName())
                .actorProfilePicture(notification.getActor().getProfilePicture())
                .referenceId(notification.getReferenceId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}