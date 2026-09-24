package org.revature.revconnect.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsResponse {

    private Long totalViews;
    private Long totalLikes;
    private Long totalComments;
    private Long totalShares;
    private Long totalImpressions;
    private Long totalFollowers;
    private Long totalPosts;
    private Double engagementRate;
    private List<DailyAnalytics> dailyData;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyAnalytics {
        private LocalDate date;
        private Integer views;
        private Integer likes;
        private Integer comments;
        private Integer shares;
        private Integer impressions;
    }
}
