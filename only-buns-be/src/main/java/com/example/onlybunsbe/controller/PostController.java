package com.example.onlybunsbe.controller;

import com.example.onlybunsbe.DTO.CommentDTO;
import com.example.onlybunsbe.DTO.PostDTO;
import com.example.onlybunsbe.model.Post;
import com.example.onlybunsbe.service.ImageService;
import com.example.onlybunsbe.service.PostService;
import com.example.onlybunsbe.repository.FollowRepository;
import com.example.onlybunsbe.util.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.onlybunsbe.dtomappers.PostMapper;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;
    @Autowired
    private TokenUtils tokenUtils;
    @Autowired
    private PostMapper postMapper;
    @Autowired
    private FollowRepository followRepository;

    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable Long id) {
        return postService.getPostById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping
    public ResponseEntity<List<PostDTO>> getAllPosts() {
        List<PostDTO> posts = postService.getAllPosts();
        return ResponseEntity.ok(posts);
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> likePost(@PathVariable Long postId, @RequestParam Long userId) {
        if (postService.likePost(postId, userId)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(409).build(); // Conflict ako je već lajkovano
    }

    @PostMapping("/{postId}/comment")
    public ResponseEntity<CommentDTO> addComment(@PathVariable Long postId, @RequestParam Long userId, @RequestBody String content) {
        Optional<CommentDTO> comment = postService.addComment(postId, userId, content);
        return comment.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostDTO> updatePost(@PathVariable Long postId, @RequestParam Long userId, @RequestBody String newDescription) {
        System.out.print(newDescription);
        Optional<PostDTO> updatedPost = postService.updatePost(postId, userId, newDescription);
        return updatedPost.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(403).build()); // Forbidden ako korisnik nije vlasnik
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId, @RequestParam Long userId) {
        if (postService.deletePost(postId, userId)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(403).build(); // Forbidden ako korisnik nije vlasnik
    }

    @PostMapping(consumes = {"multipart/form-data"}, path = "/create")
    public ResponseEntity<PostDTO> createPost(
            @RequestPart("post") PostDTO postDTO,
            @RequestPart("image") MultipartFile image, HttpServletRequest request) {
        try {

            long userId = tokenUtils.getUserIdFromToken(tokenUtils.getToken(request));
            postDTO.setUserId(userId);
            return postService.createPost(postDTO, image)
                    .map(createdPost -> ResponseEntity.status(HttpStatus.CREATED).body(createdPost))
                    .orElse(ResponseEntity.badRequest().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null); // loš format ili prevelika veličina
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // greška pri pisanju slike
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostDTO>> getPostsByUser(@PathVariable Long userId) {
        List<Post> posts = postService.getPostsByUser(userId);
        List<PostDTO> postDTOs = posts.stream()
                .map(postMapper::toPostDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(postDTOs);
    }

    @GetMapping("/user-feed")
    public ResponseEntity<List<PostDTO>> getUserFeed(@RequestParam Long userId) {
        List<PostDTO> postDTOs = postService.getUserFeed(userId);
        return ResponseEntity.ok(postDTOs);
    }
    @PutMapping("/ad-eligibility/{postId}")
    public ResponseEntity<PostDTO> markPostAsEligibleForAd(@PathVariable Long postId) {
        PostDTO updatedPost = postService.markPostAsEligible(postId);
        return ResponseEntity.ok(updatedPost); // Vraćamo ažurirani PostDTO
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<PostDTO>> getNearbyPosts(
            @RequestParam double centerLat,
            @RequestParam double centerLng,
            @RequestParam(defaultValue = "5") double radiusKm
    ) {
        List<PostDTO> posts = postService.findPostsNearby(centerLat, centerLng, radiusKm);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/in-bounds")
    public ResponseEntity<List<PostDTO>> getPostsInBounds(
            @RequestParam double minLat,
            @RequestParam double maxLat,
            @RequestParam double minLng,
            @RequestParam double maxLng
    ) {
        List<PostDTO> posts = postService.findPostsInBounds(minLat, maxLat, minLng, maxLng);
        return ResponseEntity.ok(posts);
    }

}
