package com.example.onlybunsbe.service;

import com.example.onlybunsbe.DTO.PostDTO;
import com.example.onlybunsbe.DTO.TopLikerDTO;
import com.example.onlybunsbe.DTO.TrendsSummaryDTO;
import com.example.onlybunsbe.dtomappers.PostMapper;
import com.example.onlybunsbe.model.Post;
import com.example.onlybunsbe.repository.LikeRepository;
import com.example.onlybunsbe.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrendsService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final PostMapper postMapper;

    private static final String CACHE_SUMMARY = "trends:summary";
    private static final String CACHE_TOP_7D = "trends:topPosts7d";
    private static final String CACHE_TOP_ALL = "trends:topPostsAll";
    private static final String CACHE_TOP_LIKERS_7D = "trends:topLikers7d";

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE_SUMMARY, key = "'summary'")
    public TrendsSummaryDTO getSummary() {
        long total = postRepository.count();
        Instant since30 = Instant.now().minus(30, ChronoUnit.DAYS);
        long last30 = postRepository.countByCreatedAtAfter(since30);

        List<PostDTO> top5 = getTopPostsLast7DaysInternal();
        List<PostDTO> top10All = getTopPostsAllTimeInternal();
        List<TopLikerDTO> topLikers = getTopLikers7DaysInternal();

        return TrendsSummaryDTO.builder()
                .totalPosts(total)
                .postsLast30Days(last30)
                .top5Last7Days(top5)
                .top10AllTime(top10All)
                .topLikers7Days(topLikers)
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE_TOP_7D, key = "'top5'")
    public List<PostDTO> getTopPostsLast7Days() {
        return getTopPostsLast7DaysInternal();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE_TOP_ALL, key = "'top10'")
    public List<PostDTO> getTopPostsAllTime() {
        return getTopPostsAllTimeInternal();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE_TOP_LIKERS_7D, key = "'top10'")
    public List<TopLikerDTO> getTopLikers7Days() {
        return getTopLikers7DaysInternal();
    }


    // Top 5 u poslednjih 7 dana
    private List<PostDTO> getTopPostsLast7DaysInternal() {
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        var rows = postRepository.findTopByLikesSince(since, PageRequest.of(0, 5));
        return rows.stream().map(v -> postMapper.toPostDTO(v.getPost())).toList();
    }

    // Top 10 all time -> pošalji EPOCH umesto null!
    private List<PostDTO> getTopPostsAllTimeInternal() {
        Instant since = Instant.EPOCH; // 1970-01-01T00:00:00Z
        var rows = postRepository.findTopByLikesSince(since, PageRequest.of(0, 10));
        return rows.stream().map(v -> postMapper.toPostDTO(v.getPost())).toList();
    }

    private List<TopLikerDTO> getTopLikers7DaysInternal() {
        var since7 = Instant.now().minus(7, ChronoUnit.DAYS);
        var rows = likeRepository.findTopLikersSince(since7, PageRequest.of(0, 10));
        return rows.stream().map(r -> new TopLikerDTO(r.getUserId(), r.getUsername(), r.getLikeCount()))
                .toList();
    }

    // invalidacija keša kad promenimo stanje
    @CacheEvict(cacheNames = {CACHE_SUMMARY, CACHE_TOP_7D, CACHE_TOP_ALL, CACHE_TOP_LIKERS_7D}, allEntries = true)
    public void invalidateTrendsCache() {}
}
