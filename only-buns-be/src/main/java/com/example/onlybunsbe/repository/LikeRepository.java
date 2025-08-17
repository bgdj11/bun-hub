package com.example.onlybunsbe.repository;

import com.example.onlybunsbe.model.Like;
import com.example.onlybunsbe.model.Post;
import com.example.onlybunsbe.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByPostAndUser(Post post, User user);

    void deleteByPost(Post post);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.post = :post AND l.likedAt > :afterDate")
    long countLikesAfterDate(Post post, Instant afterDate);

    interface UserLikeCountView {
        Long getUserId();
        String getUsername();
        long getLikeCount();
    }

    @Query("""
           select u.id as userId, u.username as username, count(l) as likeCount
           from Like l
           join l.user u
           where l.likedAt >= :since
           group by u.id, u.username
           order by count(l) desc
           """)
    List<UserLikeCountView> findTopLikersSince(@Param("since") Instant since, Pageable pageable);

}
