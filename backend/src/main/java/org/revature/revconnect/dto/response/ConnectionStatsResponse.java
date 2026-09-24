package org.revature.revconnect.dto.response;

import lombok.*;

import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionStatsResponse {

    private Long userId;
    private long followersCount;
    private long followingCount;

    @JsonProperty("isFollowing")
    private boolean isFollowing;

    @JsonProperty("isFollowedBy")
    private boolean isFollowedBy;
}
