package com.example.onlybunsbe.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.example.onlybunsbe.DTO.PostDTO;
import com.example.onlybunsbe.DTO.UserDTO;
import com.example.onlybunsbe.DTO.UserRequest;
import com.example.onlybunsbe.dtomappers.UserMapper;
import com.example.onlybunsbe.dtomappers.PostMapper;
import com.example.onlybunsbe.model.Follow;
import com.example.onlybunsbe.model.Role;
import com.example.onlybunsbe.model.User;
import com.example.onlybunsbe.model.Location;
import com.example.onlybunsbe.repository.PostRepository;
import com.example.onlybunsbe.repository.FollowRepository;
import com.example.onlybunsbe.repository.UserRepository;
import com.example.onlybunsbe.repository.LocationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private LocationRepository locationRepository; // ✅ dodato
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RoleService roleService;
    @Autowired private EmailSenderService emailSenderService;
    @Autowired private PostMapper postMapper;
    @Autowired private FollowRepository followRepository;

    // Pronalazi korisnika po korisničkom imenu
    @Transactional(readOnly = true)
    public User findByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username);
    }

    public User findByEmail(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email);
    }

    // Pronalazi korisnika po ID-u
    @Transactional(readOnly = true)
    public User findById(Long id) throws AccessDeniedException {
        return userRepository.findById(id).orElse(null);
    }

    // Pronalazi sve korisnike
    public List<User> findAll() throws AccessDeniedException {
        return userRepository.findAll();
    }

    // ✅ Registracija sa Location objektom
    @Transactional
    public User save(UserRequest userRequest) {
        User u = new User();
        u.setUsername(userRequest.getUsername());
        u.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        u.setFirstName(userRequest.getFirstname());
        u.setLastName(userRequest.getLastname());
        u.setEnabled(true);
        u.setEmail(userRequest.getEmail());

        // ✅ upis/vezivanje lokacije (analogno PostService)
        if (userRequest.getLocation() != null) {
            var dto = userRequest.getLocation();

            Location loc = new Location();
            loc.setCountry(dto.getCountry());
            loc.setCity(dto.getCity());
            loc.setAddress(dto.getAddress());
            loc.setNumber(dto.getNumber());
            loc.setLatitude(dto.getLatitude());
            loc.setLongitude(dto.getLongitude());

            // može i bez ovog poziva ako staviš cascade = PERSIST na relaciji u User
            locationRepository.save(loc);
            u.setLocation(loc);
        }

        Role role = roleService.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Role 'ROLE_USER' not found"));
        u.setRole(role);
        u.setEnabled(false);

        u.setActivationToken(UUID.randomUUID().toString());
        User savedUser = userRepository.save(u);

        String activationLink = "http://localhost:8080/auth/activate?token=" + u.getActivationToken();
        emailSenderService.sendEmail(
                u.getEmail(),
                "Account Activation",
                "Click the following link to activate your account: " + activationLink
        );

        return savedUser;
    }

    // Filtriranje/sortiranje korisnika -> vraća DTO (sa location)
    public List<UserDTO> getAllUsers(String name, String email, Integer minPosts, Integer maxPosts, String sortBy, boolean isAscending) {
        List<User> users = userRepository.findAll();

        Stream<User> filteredUsers = users.stream()
                .filter(user -> {
                    boolean nameMatch = (name == null || user.getFirstName().contains(name) || user.getLastName().contains(name));
                    boolean emailMatch = (email == null || user.getEmail().contains(email));
                    boolean minPostsMatch = (minPosts == null || (user.getPosts() != null && user.getPosts().size() >= minPosts));
                    boolean maxPostsMatch = (maxPosts == null || (user.getPosts() != null && user.getPosts().size() <= maxPosts));
                    return nameMatch && emailMatch && minPostsMatch && maxPostsMatch;
                });

        Comparator<User> comparator = "followers".equals(sortBy)
                ? Comparator.comparingInt(user -> calculateFollowersCount(user.getId()))
                : Comparator.comparing(User::getEmail);

        if (!isAscending) comparator = comparator.reversed();

        return filteredUsers
                .sorted(comparator)
                .map(UserMapper::toDTO) // ✅ sada koristi UserMapper koji mapira location
                .collect(Collectors.toList());
    }

    public boolean activateUser(String token) {
        User user = userRepository.findByActivationToken(token);
        if (user != null) {
            user.setEnabled(true);
            user.setActivationToken(null);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public List<User> getUsersInactiveForMoreThan7Days() {
        int daysInactive = 7;
        Instant thresholdInstant = Instant.now().minus(daysInactive, ChronoUnit.DAYS);
        return userRepository.findUsersWithLastLoginBefore(thresholdInstant);
    }

    @Scheduled(cron = "0 59 23 L * ?")
    public void deleteUnactivatedAccounts() {
        List<User> unactivatedUsers = userRepository.findAll().stream()
                .filter(user -> !user.isEnabled())
                .collect(Collectors.toList());

        if (!unactivatedUsers.isEmpty()) {
            userRepository.deleteAll(unactivatedUsers);
            System.out.println("Deleted unactivated accounts: " + unactivatedUsers.size());
        } else {
            System.out.println("No unactivated accounts found for deletion.");
        }
    }

    public void followUser(String currentUsername, Long userIdToFollow) {
        User currentUser = userRepository.findByUsername(currentUsername);
        User userToFollow = userRepository.findById(userIdToFollow)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (followRepository.existsByFollowerAndFollowed(currentUser, userToFollow)) {
            throw new RuntimeException("Already following this user");
        }

        Follow follow = new Follow();
        follow.setFollower(currentUser);
        follow.setFollowed(userToFollow);
        followRepository.save(follow);
    }

    public void unfollowUser(String currentUsername, Long userIdToUnfollow) {
        User currentUser = userRepository.findByUsername(currentUsername);
        User userToUnfollow = userRepository.findById(userIdToUnfollow)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Follow follow = followRepository.findByFollowerAndFollowed(currentUser, userToUnfollow)
                .orElseThrow(() -> new RuntimeException("Not following this user"));

        followRepository.delete(follow);
    }

    public List<PostDTO> getFeed(String currentUsername) {
        User currentUser = userRepository.findByUsername(currentUsername);

        Set<User> following = followRepository.findAllByFollower(currentUser).stream()
                .map(Follow::getFollowed)
                .collect(Collectors.toSet());

        return postRepository.findByUserIn(following).stream()
                .map(postMapper::toPostDTO)
                .collect(Collectors.toList());
    }

    public List<UserDTO> getFollowing(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        return followRepository.findAllByFollower(user).stream()
                .map(follow -> UserMapper.toDTO(follow.getFollowed()))
                .collect(Collectors.toList());
    }

    public List<UserDTO> getFollowers(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        return followRepository.findAllByFollowed(user).stream()
                .map(follow -> UserMapper.toDTO(follow.getFollower()))
                .collect(Collectors.toList());
    }

    private int calculateFollowersCount(Long userId) {
        return (int) userRepository.findAll().stream()
                .filter(u -> u.getFollowing().stream()
                        .anyMatch(f -> f.getId().equals(userId)))
                .count();
    }

    public void updateLastLogin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));
        user.setLastLoginDate(Instant.now());
        userRepository.save(user);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Pogrešna trenutna lozinka.");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Nova lozinka mora imati najmanje 8 karaktera.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
