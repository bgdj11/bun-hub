package com.example.onlybunsbe.repository;

import com.example.onlybunsbe.model.Post;
import com.example.onlybunsbe.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByOrderByCreatedAtDesc(); // Dodaje sortiranje postova po datumu kreiranja
    @Query("SELECT COUNT(p) FROM Post p WHERE p.createdAt >= :date")
    long countPostsAfterDate(@Param("date") Instant date);
    List<Post> findByUserIn(Set<User> users);

    long count();
    long countByCreatedAtAfter(Instant since);
    interface PostLikeCountView {
        Post getPost();
        long getLikeCount();
    }

    @Query("""
   select p as post,
          sum(case when l.likedAt >= :since then 1 else 0 end) as likeCount
   from Post p
   left join p.likes l
   group by p
   order by sum(case when l.likedAt >= :since then 1 else 0 end) desc, p.createdAt desc
   """)
    List<PostLikeCountView> findTopByLikesSince(@Param("since") Instant since, Pageable pageable);

    List<Post> findByUserId(Long userId);

    @Query("SELECT p FROM Post p WHERE p.user.id IN :userIds ORDER BY p.createdAt DESC")
    List<Post> findByUserIdInOrderByCreatedAtDesc(@Param("userIds") List<Long> userIds);

    @Query("""
        SELECT p
        FROM Post p
        WHERE p.location.latitude BETWEEN :minLat AND :maxLat
          AND p.location.longitude BETWEEN :minLng AND :maxLng
        ORDER BY p.createdAt DESC
    """)
    List<Post> findInBounds(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng
    );

}


