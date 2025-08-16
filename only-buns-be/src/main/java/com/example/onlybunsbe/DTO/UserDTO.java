package com.example.onlybunsbe.DTO;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDTO {
    private Long id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private LocationDTO location;
    private boolean isActive;
    private LocalDateTime registeredAt;
    private String role;
    private int postCount;         // Broj objava
    private int followingCount;    // Broj korisnika koje prati
    private int followerCount;     // Broj pratilaca

    private List<Long> sentMessageIds;    // ID-evi poslatih poruka
    private List<Long> groupIds;          // ID-evi grupa u kojima je korisnik član
}
