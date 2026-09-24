package org.revature.revconnect.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hashtags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false)
    private Long usageCount;

    private LocalDateTime lastUsed;

    @PrePersist
    protected void onCreate() {
        usageCount = 1L;
        lastUsed = LocalDateTime.now();
    }

    public void incrementUsage() {
        usageCount++;
        lastUsed = LocalDateTime.now();
    }
}
