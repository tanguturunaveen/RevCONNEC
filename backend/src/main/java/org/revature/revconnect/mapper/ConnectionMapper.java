package org.revature.revconnect.mapper;

import org.revature.revconnect.dto.response.ConnectionResponse;
import org.revature.revconnect.model.Connection;
import org.springframework.stereotype.Component;

@Component
public class ConnectionMapper {

    public ConnectionResponse fromFollower(Connection connection) {
        return ConnectionResponse.builder()
                .id(connection.getId())
                .userId(connection.getFollower().getId())
                .username(connection.getFollower().getUsername())
                .name(connection.getFollower().getName())
                .profilePicture(connection.getFollower().getProfilePicture())
                .bio(connection.getFollower().getBio())
                .status(connection.getStatus())
                .createdAt(connection.getCreatedAt())
                .build();
    }

    public ConnectionResponse fromFollowing(Connection connection) {
        return ConnectionResponse.builder()
                .id(connection.getId())
                .userId(connection.getFollowing().getId())
                .username(connection.getFollowing().getUsername())
                .name(connection.getFollowing().getName())
                .profilePicture(connection.getFollowing().getProfilePicture())
                .bio(connection.getFollowing().getBio())
                .status(connection.getStatus())
                .createdAt(connection.getCreatedAt())
                .build();
    }
}