package com.example.onlybunsbe.service;

import com.example.onlybunsbe.DTO.CommentDTO;
import com.example.onlybunsbe.DTO.ImageDTO;
import com.example.onlybunsbe.DTO.LocationDTO;
import com.example.onlybunsbe.DTO.PostDTO;
import com.example.onlybunsbe.infrastructure.messaging.CustomQueueClient;
import com.example.onlybunsbe.model.*;
import com.example.onlybunsbe.infrastructure.messaging.RabbitMQPublisher;
import com.example.onlybunsbe.model.Comment;
import com.example.onlybunsbe.model.Like;
import com.example.onlybunsbe.repository.*;
import com.example.onlybunsbe.dtomappers.PostMapper;
import com.example.onlybunsbe.model.Comment;
import com.example.onlybunsbe.model.Like;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private ImageService imageService;
    private UserRepository userRepository;
    private LikeRepository likeRepository;
    private CommentRepository commentRepository;
    private LocationRepository locationRepository;
    private FollowRepository followRepository;
    private final RabbitMQPublisher rabbitMQPublisher; // Dodato
    private final TrendsService trendsService;
    private final CustomQueueClient customQueueClient;
    @Transactional
    public Optional<PostDTO> getPostById(Long id) {
        return postRepository.findById(id).map(postMapper::toPostDTO);
    }

    @Transactional
    public List<PostDTO> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(postMapper::toPostDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<PostDTO> createPost(PostDTO postDTO, MultipartFile image) throws IOException {
        log.info("createPost called with dto={}, imageName={}, imageSize={}",
                postDTO, image != null ? image.getOriginalFilename() : null,
                image != null ? image.getSize() : null);

        // Create Image entity
        Image imageEntity = imageService.createImageEntity(image);
        log.debug("Image entity created id={}, path={}", imageEntity.getId(), imageEntity.getPath());

        if (postDTO.getImage() != null) {
            postDTO.getImage().setPath(imageEntity.getPath());
        } else {
            log.warn("postDTO.getImage() is null, creating new ImageDTO");
            postDTO.setImage(new com.example.onlybunsbe.DTO.ImageDTO());
            postDTO.getImage().setPath(imageEntity.getPath());
        }

        // Create Post entity
        Post post = postMapper.toPostEntity(postDTO);
        post.setComments(new ArrayList<>());
        post.setLikes(new ArrayList<>());
        post.setImage(imageEntity);

        // Save location if new
        if (post.getLocation() != null && post.getLocation().getId() == null) {
            locationRepository.save(post.getLocation());
            log.debug("Location saved id={}, city={}, country={}",
                    post.getLocation().getId(), post.getLocation().getCity(), post.getLocation().getCountry());
        }

        // Save post
        Post savedPost = postRepository.save(post);
        log.info("Post saved id={}, userId={}, desc={}",
                savedPost.getId(),
                savedPost.getUser() != null ? savedPost.getUser().getId() : null,
                savedPost.getDescription());

        trendsService.invalidateTrendsCache();
        log.debug("Trends cache invalidated after creating post {}", savedPost.getId());

        PostDTO result = postMapper.toPostDTO(savedPost);
        log.info("Returning created PostDTO id={}", result.getId());

        return Optional.of(result);
    }

    @Transactional
    public boolean likePost(Long postId, Long userId) {
        log.info("likePost called postId={}, userId={}", postId, userId);
        var post = postRepository.findById(postId);
        var user = userRepository.findById(userId);

        if (post.isPresent() && user.isPresent()) {
            if (likeRepository.existsByPostAndUser(post.get(), user.get())) {
                log.warn("User {} already liked post {}", userId, postId);
                return false;
            }
            Like like = new Like();
            like.setUser(user.get());
            like.setPost(post.get());
            like.setLikedAt(Instant.now());
            likeRepository.save(like);
            log.info("User {} liked post {}", userId, postId);
            trendsService.invalidateTrendsCache();
            return true;
        }
        log.warn("Post or user not found (postId={}, userId={})", postId, userId);
        return false;
    }

    @Transactional
    public Optional<CommentDTO> addComment(Long postId, Long userId, String content) {
        log.info("addComment called postId={}, userId={}, content={}", postId, userId, content);
        var post = postRepository.findById(postId);
        var user = userRepository.findById(userId);

        if (post.isPresent() && user.isPresent()) {
            Comment comment = new Comment();
            comment.setContent(content);
            comment.setUser(user.get());
            comment.setPost(post.get());
            comment.setCreatedAt(Instant.now());
            commentRepository.save(comment);

            CommentDTO dto = new CommentDTO();
            dto.setId((long) comment.getId());
            dto.setContent(content);
            dto.setCreatedAt(comment.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime());
            dto.setUserName(user.get().getUsername());

            log.info("Comment saved id={} for post {}", dto.getId(), postId);
            trendsService.invalidateTrendsCache();
            return Optional.of(dto);
        }
        log.warn("Failed to add comment, post or user missing (postId={}, userId={})", postId, userId);
        return Optional.empty();
    }


    // Metoda za ažuriranje objave
    @Transactional
    public Optional<PostDTO> updatePost(Long postId, Long userId, String newDescription) {
        var post = postRepository.findById(postId);

        if (post.isPresent() && post.get().getUser().getId().equals(userId)) {
            post.get().setDescription(newDescription);
            postRepository.save(post.get());
            return getPostById(postId);
        }
        return Optional.empty();
    }

    // Metoda za brisanje objave
    @Transactional
    public boolean deletePost(Long postId, Long userId) {
        var post = postRepository.findById(postId);

        if (post.isPresent() && post.get().getUser().getId().equals(userId)) {
            // Uklanjanje lajkova i komentara vezanih za post
            likeRepository.deleteByPost(post.get());
            commentRepository.deleteByPost(post.get());
            postRepository.delete(post.get());
            trendsService.invalidateTrendsCache();
            return true;
        }
        return false;
    }

    public List<Post> getPostsByUser(Long userId) {
        return postRepository.findByUserId(userId);
    }

    public List<PostDTO> getUserFeed(Long userId) {
        // Pronađi sve korisnike koje korisnik prati
        List<Long> followedUserIds = followRepository.findFollowedIdsByFollowerId(userId);
        System.out.println("Followed User IDs: " + followedUserIds);

        // Ako je lista prazna, koristimo samo ID trenutnog korisnika
        if (followedUserIds.isEmpty()) {
            System.out.println("No followed users found. Returning posts for the current user only.");
            followedUserIds = List.of(userId); // Napravimo novu listu sa samo trenutnim korisnikom
        } else {
            // Dodaj ID trenutno prijavljenog korisnika u listu
            followedUserIds.add(userId);
        }

        // Pronađi postove samo za ove korisnike
        List<Post> posts = postRepository.findByUserIdInOrderByCreatedAtDesc(followedUserIds);
        System.out.println("Posts Retrieved: " + posts);

        // Mapiraj postove u DTO
        return posts.stream()
                .map(postMapper::toPostDTO)
                .collect(Collectors.toList());
    }


    public PostDTO markPostAsEligible(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));


        post.setEligibleForAd(true);
        Post updatedPost = postRepository.save(post);


        customQueueClient.publishToExchange("ads", Map.of(
                "description", post.getDescription(),
                "username", post.getUser().getUsername(),
                "publishTime", post.getCreatedAt().toString()
        ));

        return postMapper.toPostDTO(updatedPost);
    }

    @Transactional(readOnly = true)
    public List<PostDTO> findPostsNearby(double centerLat, double centerLng, double radiusKm) {
        List<Post> all = postRepository.findAll(); // optimizuj po potrebi (specifikacija/SQL)
        double rKm = radiusKm > 0 ? radiusKm : 5.0;

        List<PostDTO> res = all.stream()
                .filter(p -> p.getLocation() != null)
                .filter(p -> {
                    var loc = p.getLocation();
                    double d = haversineKm(centerLat, centerLng, loc.getLatitude(), loc.getLongitude());
                    return d <= rKm;
                })
                .map(postMapper::toPostDTO)
                .toList();

        return res;
    }

    @Transactional(readOnly = true)
    public List<PostDTO> findPostsInBounds(double minLat, double maxLat, double minLng, double maxLng) {
        List<Post> all = postRepository.findAll();
        List<PostDTO> res = all.stream()
                .filter(p -> p.getLocation() != null)
                .filter(p -> {
                    var loc = p.getLocation();
                    return loc.getLatitude()  >= minLat && loc.getLatitude()  <= maxLat
                            && loc.getLongitude() >= minLng && loc.getLongitude() <= maxLng;
                })
                .map(postMapper::toPostDTO)
                .toList();

        return res;
    }


    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

}
