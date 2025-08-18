package com.example.onlybunsbe.service;

import com.example.onlybunsbe.DTO.LikeDTO;
import com.example.onlybunsbe.DTO.PostDTO;
import com.example.onlybunsbe.dtomappers.PostMapper;
import com.example.onlybunsbe.model.Like;
import com.example.onlybunsbe.model.Post;
import com.example.onlybunsbe.model.User;
import com.example.onlybunsbe.repository.LikeRepository;
import com.example.onlybunsbe.repository.PostRepository;
import com.example.onlybunsbe.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    private final PostMapper postMapper;
    private final TrendsService trendsService;

    @Transactional
    public PostDTO likePost(Long postId, Long userId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (postOpt.isPresent() && userOpt.isPresent()) {
            Post post = postOpt.get();
            User user = userOpt.get();

            if (likeRepository.existsByPostAndUser(post, user)) {
                return null;
            }

            // Dodajemo novu instancu lajkova
            Like like = new Like();
            like.setUser(user);
            like.setLikedAt(Instant.now());


            // Simulacija konflikta sa Thread.sleep()
            //try {
            //    Thread.sleep(10000);
           // } catch (InterruptedException e) {
              //  Thread.currentThread().interrupt();
           // }


            post.addLike(like);
            postRepository.save(post);
            trendsService.invalidateTrendsCache();
            return postMapper.toPostDTO(post);
        }

        return null;
    }


    @Transactional
    public void removeLikesByPost(Post post) {
        likeRepository.deleteByPost(post);
    }

    @Transactional
    public List<LikeDTO> getAllLikes() {
        return likeRepository.findAll().stream().map(like -> {
            LikeDTO dto = new LikeDTO();
            dto.setPostId(Long.valueOf(like.getPost().getId()));
            dto.setUserId(like.getUser().getId());
            return dto;
        }).collect(Collectors.toList());
    }

    public long getLikeCountAfterDate(Post post, Instant afterDate) {
        return likeRepository.countLikesAfterDate(post, afterDate);
    }
}
