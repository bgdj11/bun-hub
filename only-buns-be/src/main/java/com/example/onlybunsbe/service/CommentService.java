package com.example.onlybunsbe.service;

import com.example.onlybunsbe.DTO.CommentDTO;
import com.example.onlybunsbe.model.Comment;
import com.example.onlybunsbe.model.Post;
import com.example.onlybunsbe.model.User;
import com.example.onlybunsbe.repository.CommentRepository;
import com.example.onlybunsbe.repository.PostRepository;
import com.example.onlybunsbe.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

@Service
@AllArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final TrendsService trendsService;



    @Transactional
    public Optional<CommentDTO> addComment(CommentDTO commentDTO) {
        Optional<Post> post = postRepository.findById(commentDTO.getPostId());
        Optional<User> user = userRepository.findById(commentDTO.getUserId());
        System.out.print("Pronadjen post = " + post + " user = " + user);
        if (post.isPresent() && user.isPresent()) {
            Comment comment = new Comment();
            comment.setPost(post.get());
            comment.setUser(user.get());
            comment.setContent(commentDTO.getContent());
            comment.setCreatedAt(Instant.now());
            commentRepository.save(comment);

            trendsService.invalidateTrendsCache();

            // Mapiranje komentara u CommentDTO
            CommentDTO result = new CommentDTO();
            result.setId((long) comment.getId());
            result.setContent(commentDTO.getContent());
            result.setCreatedAt(comment.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime());
            result.setUserName(user.get().getUsername());

            return Optional.of(result);
        }
        return Optional.empty();
    }

    @Transactional
    public void removeCommentsByPost(Post post) {
        commentRepository.deleteByPost(post);
    }

    public long getCommentCountAfterDate(Post post, Instant afterDate) {
        return commentRepository.countCommentsAfterDate(post, afterDate);
    }
}
