package org.revature.revconnect.dto.response;

import org.revature.revconnect.enums.ConnectionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionResponse {

    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String profilePicture;
    private String bio;
    private ConnectionStatus status;
    private LocalDateTime createdAt;
}
