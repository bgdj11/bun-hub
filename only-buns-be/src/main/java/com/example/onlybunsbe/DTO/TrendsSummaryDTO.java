package com.example.onlybunsbe.DTO;

import lombok.*;
import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class TrendsSummaryDTO {
    private long totalPosts;
    private long postsLast30Days;
    private List<PostDTO> top5Last7Days;
    private List<PostDTO> top10AllTime;
    private List<TopLikerDTO> topLikers7Days;
}
