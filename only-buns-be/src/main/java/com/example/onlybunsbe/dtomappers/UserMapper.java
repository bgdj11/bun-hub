package com.example.onlybunsbe.dtomappers;

import com.example.onlybunsbe.DTO.LocationDTO;
import com.example.onlybunsbe.DTO.UserDTO;
import com.example.onlybunsbe.model.ChatMessage;
import com.example.onlybunsbe.model.Follow;
import com.example.onlybunsbe.model.Location;
import com.example.onlybunsbe.model.User;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

public class UserMapper {

    public static UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();

        // Osnovna polja
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());

        // ❌ dto.setAddress(user.getAddress());
        // ✅ location
        if (user.getLocation() != null) {
            dto.setLocation(toLocationDTO(user.getLocation()));
        }

        // Datum (ovde koristiš lastLoginDate kao registeredAt; ostaviću kako je bilo)
        dto.setRegisteredAt(user.getLastLoginDate() != null
                ? LocalDateTime.ofInstant(user.getLastLoginDate(), ZoneId.systemDefault())
                : null);

        // Rola
        dto.setRole(user.getRole() != null ? user.getRole().getName() : null);

        // Statistika
        dto.setPostCount(user.getPosts() != null ? user.getPosts().size() : 0);
        dto.setFollowingCount(user.getFollowing() != null ? user.getFollowing().size() : 0);
        dto.setFollowerCount(user.getFollowers() != null ? user.getFollowers().size() : 0);

        // Poruke
        dto.setSentMessageIds(user.getSentMessages() != null
                ? user.getSentMessages().stream().map(ChatMessage::getId).collect(Collectors.toList())
                : List.of());

        return dto;
    }

    public static UserDTO toUserFollowDTO(User user, List<Follow> followers, List<Follow> following) {
        UserDTO dto = new UserDTO();

        // Osnovna polja
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());

        if (user.getLocation() != null) {
            dto.setLocation(toLocationDTO(user.getLocation()));
        }

        // Datum
        dto.setRegisteredAt(user.getLastLoginDate() != null
                ? LocalDateTime.ofInstant(user.getLastLoginDate(), ZoneId.systemDefault())
                : null);

        // Rola
        dto.setRole(user.getRole() != null ? user.getRole().getName() : null);

        // Statistika
        dto.setPostCount(user.getPosts() != null ? user.getPosts().size() : 0);
        dto.setFollowingCount(following != null ? following.size() : 0);
        dto.setFollowerCount(followers != null ? followers.size() : 0);

        // Poruke
        dto.setSentMessageIds(user.getSentMessages() != null
                ? user.getSentMessages().stream().map(ChatMessage::getId).collect(Collectors.toList())
                : List.of());

        return dto;
    }

    // Helper
    private static LocationDTO toLocationDTO(Location l) {
        LocationDTO dto = new LocationDTO();
        dto.setId(l.getId());
        dto.setCountry(l.getCountry());
        dto.setCity(l.getCity());
        dto.setAddress(l.getAddress());
        dto.setNumber(l.getNumber());
        dto.setLatitude(l.getLatitude());
        dto.setLongitude(l.getLongitude());
        return dto;
    }
}
