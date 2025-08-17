package com.example.onlybunsbe.DTO;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class TopLikerDTO {
    private Long userId;
    private String username;
    private long likeCount;
}
