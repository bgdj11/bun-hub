package com.example.onlybunsbe.repository;

import com.example.onlybunsbe.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByUsername(String username);
    User findByEmail(String email);
    User findById(long id);
    User findByActivationToken(String activationToken);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.posts WHERE u.lastLoginDate < :cutoffDate")
    List<User> findUsersWithLastLoginBefore(@Param("cutoffDate") Instant cutoffDate);

    @Query("SELECT COUNT(u) FROM User u WHERE SIZE(u.posts) > 0")
    long countByPostsIsNotEmpty();

    @Query("SELECT COUNT(DISTINCT c.user.id) " +
            "FROM Comment c " +
            "WHERE c.user.id NOT IN (SELECT p.user.id FROM Post p)")
    long countWithOnlyComments();

    long countByUsername(String username);
    void deleteByUsername(String username);

    @Query("SELECT u FROM User u JOIN u.following f WHERE f.id = :userId")
    List<User> findFollowers(@Param("userId") Long userId);
}
