package org.revature.revconnect.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings", indexes = {
        @Index(name = "idx_user_settings_user_id", columnList = "user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "notify_connection_request")
    @Builder.Default
    private Boolean notifyConnectionRequest = true;

    @Column(name = "notify_connection_accepted")
    @Builder.Default
    private Boolean notifyConnectionAccepted = true;

    @Column(name = "notify_new_follower")
    @Builder.Default
    private Boolean notifyNewFollower = true;

    @Column(name = "notify_like")
    @Builder.Default
    private Boolean notifyLike = true;

    @Column(name = "notify_comment")
    @Builder.Default
    private Boolean notifyComment = true;

    @Column(name = "notify_share")
    @Builder.Default
    private Boolean notifyShare = true;

    @Column(name = "email_notifications")
    @Builder.Default
    private Boolean emailNotifications = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
