package org.revature.revconnect.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.revature.revconnect.model.Post;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_analytics", indexes = {
        @Index(name = "idx_analytics_post", columnList = "post_id"),
        @Index(name = "idx_analytics_date", columnList = "date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    @Builder.Default
    private Integer views = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer likes = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer comments = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer shares = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer clicks = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer impressions = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
