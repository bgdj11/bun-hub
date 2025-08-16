package com.example.onlybunsbe.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.onlybunsbe.DTO.GroupChatDTO;
import com.example.onlybunsbe.DTO.PostDTO;
import com.example.onlybunsbe.DTO.UserDTO;
import com.example.onlybunsbe.dtomappers.GroupChatMapper;
import com.example.onlybunsbe.dtomappers.UserMapper;
import com.example.onlybunsbe.model.GroupChat;
import com.example.onlybunsbe.model.User;
import com.example.onlybunsbe.service.GroupChatService;
import com.example.onlybunsbe.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.example.onlybunsbe.DTO.ChangePasswordRequest;
import org.springframework.security.core.Authentication;
import com.example.onlybunsbe.util.TokenUtils;


// Primer kontrolera cijim metodama mogu pristupiti samo autorizovani korisnici
@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin
public class UserController {

    @Autowired
    private GroupChatService groupChatService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private TokenUtils tokenUtils;

    // Za pristup ovoj metodi neophodno je da ulogovani korisnik ima ADMIN ulogu
    // Ukoliko nema, server ce vratiti gresku 403 Forbidden
    // Korisnik jeste autentifikovan, ali nije autorizovan da pristupi resursu
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserDTO> loadById(@PathVariable Long userId) {
        User u = this.userService.findById(userId);
        if (u == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(com.example.onlybunsbe.dtomappers.UserMapper.toDTO(u));
    }

    @GetMapping("/user/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> loadAll() {
        return this.userService.findAll();
    }

    @GetMapping("/whoami")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserDTO> whoami(Principal principal) {
        User u = this.userService.findByUsername(principal.getName());
        return ResponseEntity.ok(com.example.onlybunsbe.dtomappers.UserMapper.toDTO(u));
    }

    // testna funkcija
    @GetMapping("/foo")
    public Map<String, String> getFoo() {
        Map<String, String> fooObj = new HashMap<>();
        fooObj.put("foo", "bar");
        return fooObj;
    }

    @GetMapping("/user/sort")
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @RequestParam(value = "sortBy", defaultValue = "email") String sortBy,
            @RequestParam(value = "isAscending", defaultValue = "true") boolean isAscending,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "minPosts", required = false) Integer minPosts,
            @RequestParam(value = "maxPosts", required = false) Integer maxPosts) {

        // Dodajemo logove za parametre
        System.out.println("sortBy: " + sortBy);
        System.out.println("isAscending: " + isAscending);
        System.out.println("name: " + name);
        System.out.println("email: " + email);
        System.out.println("minPosts: " + minPosts);
        System.out.println("maxPosts: " + maxPosts);

        List<UserDTO> users = userService.getAllUsers(name, email, minPosts, maxPosts, sortBy, isAscending);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/user/{userId}/groups")
    public List<GroupChatDTO> getGroupsForUser(@PathVariable Long userId) {
        List<GroupChat> groups = groupChatService.getGroupsForUser(userId);
        return groups.stream()
                .map(GroupChatMapper::toDTO)
                .collect(Collectors.toList());
    }

    @PostMapping("/user/{userId}/follow")
    public ResponseEntity<String> followUser(@PathVariable Long userId, Principal principal) {
        userService.followUser(principal.getName(), userId);
        return ResponseEntity.ok("Started following user");
    }

    @DeleteMapping("/user/{userId}/unfollow")
    public ResponseEntity<String> unfollowUser(@PathVariable Long userId, Principal principal) {
        userService.unfollowUser(principal.getName(), userId);
        return ResponseEntity.ok("Stopped following user");
    }

    @GetMapping("/user/feed")
    public List<PostDTO> getFeed(Principal principal) {
        return userService.getFeed(principal.getName());
    }

    @GetMapping("/user/{userId}/following")
    public List<UserDTO> getFollowing(@PathVariable Long userId) {
        return userService.getFollowing(userId);
    }

    @GetMapping("/user/{userId}/followers")
    public List<UserDTO> getFollowers(@PathVariable Long userId) {
        return userService.getFollowers(userId);
    }

    @GetMapping("/user/searchEmail")
    public ResponseEntity<UserDTO> getUserByEmail(@RequestParam String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        UserDTO userDTO = UserMapper.toDTO(user);
        return ResponseEntity.ok(userDTO);
    }

    @PutMapping("/user/password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest req,
                                            HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token nije prosleđen.");
            }
            String token = authHeader.substring(7);
            String email = tokenUtils.getEmailFromToken(token);

            User user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Korisnik nije pronađen.");
            }

            if (!req.getNewPassword().equals(req.getConfirmNewPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lozinke se ne poklapaju.");
            }

            userService.changePassword(user.getId(), req.getCurrentPassword(), req.getNewPassword());

            return ResponseEntity.ok(Map.of("message", "Lozinka uspešno promenjena."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Došlo je do greške.");
        }
    }

}
