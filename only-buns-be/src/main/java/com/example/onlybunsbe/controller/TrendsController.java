package com.example.onlybunsbe.controller;

import com.example.onlybunsbe.DTO.PostDTO;
import com.example.onlybunsbe.DTO.TopLikerDTO;
import com.example.onlybunsbe.DTO.TrendsSummaryDTO;
import com.example.onlybunsbe.service.TrendsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trends")
public class TrendsController {

    private final TrendsService trendsService;

    @GetMapping("/summary")
    public ResponseEntity<TrendsSummaryDTO> getSummary() {
        return ResponseEntity.ok(trendsService.getSummary());
    }

    @GetMapping("/top-posts/last7days")
    public ResponseEntity<List<PostDTO>> getTopPostsLast7Days() {
        return ResponseEntity.ok(trendsService.getTopPostsLast7Days());
    }

    @GetMapping("/top-posts/alltime")
    public ResponseEntity<List<PostDTO>> getTopPostsAllTime() {
        return ResponseEntity.ok(trendsService.getTopPostsAllTime());
    }

    @GetMapping("/top-likers/last7days")
    public ResponseEntity<List<TopLikerDTO>> getTopLikersLast7Days() {
        return ResponseEntity.ok(trendsService.getTopLikers7Days());
    }
}
