package com.example.onlybunsbe.dtomappers;

import com.example.onlybunsbe.DTO.CommentDTO;
import com.example.onlybunsbe.DTO.ImageDTO;
import com.example.onlybunsbe.DTO.LocationDTO;
import com.example.onlybunsbe.DTO.PostDTO;
import com.example.onlybunsbe.model.Image;
import com.example.onlybunsbe.model.Location;
import com.example.onlybunsbe.model.Post;
import com.example.onlybunsbe.model.User;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PostMapper {

    public Post toPostEntity(PostDTO postDTO) {
        Post post = new Post();
        post.setDescription(postDTO.getDescription());
        // backend postavlja vreme, ne oslanjaj se na front
        post.setCreatedAt(Instant.now());

        // Image: putanja se već setuje u servisu nakon imageService.createImageEntity(...)
        // OVDЕ NE PRAVI NOVI Image (da ne praviš “duplu” instancu).
        // post.setImage(...) radi PostService posle kreiranja imageEntity

        // Location (dozvoljeno napraviti novi jer ga posle čuvaš u repo ako je nov):
        if (postDTO.getLocation() != null) {
            Location location = new Location();
            location.setLatitude(postDTO.getLocation().getLatitude());
            location.setLongitude(postDTO.getLocation().getLongitude());
            location.setCity(postDTO.getLocation().getCity());
            location.setCountry(postDTO.getLocation().getCountry());
            location.setNumber(postDTO.getLocation().getNumber());
            location.setAddress(postDTO.getLocation().getAddress());
            post.setLocation(location);
        }

        if (postDTO.getUserId() != null) {
            User user = new User();
            user.setId(postDTO.getUserId());
            post.setUser(user);
        }

        post.setEligibleForAd(postDTO.isEligibleForAd());
        return post;
    }

    public PostDTO toPostDTO(Post post) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId() != null ? post.getId().longValue() : null);
        dto.setDescription(post.getDescription());
        dto.setCreatedAt(
                post.getCreatedAt() != null
                        ? post.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : null
        );

        // Image (plitko)
        if (post.getImage() != null) {
            ImageDTO imageDTO = new ImageDTO();
            imageDTO.setPath(post.getImage().getPath());
            dto.setImage(imageDTO);
        }

        // Location (plitko)
        if (post.getLocation() != null) {
            LocationDTO locationDTO = new LocationDTO();
            locationDTO.setLatitude(post.getLocation().getLatitude());
            locationDTO.setLongitude(post.getLocation().getLongitude());
            locationDTO.setCity(post.getLocation().getCity());
            locationDTO.setCountry(post.getLocation().getCountry());
            locationDTO.setAddress(post.getLocation().getAddress());
            locationDTO.setNumber(post.getLocation().getNumber());
            dto.setLocation(locationDTO);
        }

        // likeCount — bez ulaska u Like.user
        dto.setLikeCount(post.getLikes() != null ? post.getLikes().size() : 0);

        // komentari — mapiraj PLITKO; korisnik može biti null ili LAZY
        if (post.getComments() != null) {
            List<CommentDTO> commentDTOs = post.getComments().stream().map(comment -> {
                CommentDTO commentDTO = new CommentDTO();
                commentDTO.setId(comment.getId() != null ? comment.getId().longValue() : null);
                commentDTO.setPostId(post.getId() != null ? post.getId().longValue() : null);
                if (comment.getUser() != null) {
                    commentDTO.setUserId(comment.getUser().getId() != null ? comment.getUser().getId().longValue() : null);
                    commentDTO.setUserName(comment.getUser().getUsername());
                }
                commentDTO.setContent(comment.getContent());
                commentDTO.setCreatedAt(
                        comment.getCreatedAt() != null
                                ? comment.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime()
                                : null
                );
                return commentDTO;
            }).collect(Collectors.toList());
            dto.setComments(commentDTOs);
        }

        if (post.getUser() != null) {
            dto.setUserId(post.getUser().getId() != null ? post.getUser().getId().longValue() : null);
        }

        dto.setEligibleForAd(Boolean.TRUE.equals(post.getEligibleForAd()));
        return dto;
    }
}

