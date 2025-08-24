package com.example.onlybunsbe.service;

import com.example.onlybunsbe.DTO.JwtAuthenticationRequest;
import com.example.onlybunsbe.DTO.UserRequest;
import com.example.onlybunsbe.DTO.UserTokenState;
import com.example.onlybunsbe.exception.ResourceConflictException;
import com.example.onlybunsbe.infrastructure.bloom.UsernameBloomService;
import com.example.onlybunsbe.model.User;
import com.example.onlybunsbe.util.TokenUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService {

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired private UsernameBloomService usernameBloom;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private AttemptService attemptService;



    public ResponseEntity<?> login(JwtAuthenticationRequest authenticationRequest, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();

        // Check if the IP address is blocked
        if (attemptService.isBlocked(ipAddress)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many login attempts. Please try again later.");
        }

        try {
            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getEmail(), authenticationRequest.getPassword())
            );

            // Reset failed login attempts
            attemptService.clearAttempts(ipAddress);

            // Set security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate JWT token
            User user = (User) authentication.getPrincipal();
            String jwt = tokenUtils.generateToken(user.getEmail(), user.getId(), user.getRole().getName());
            int expiresIn = tokenUtils.getExpiredIn();

            userService.updateLastLogin(user.getId());

            return ResponseEntity.ok(new UserTokenState(jwt, expiresIn, user.getId()));

        } catch (BadCredentialsException ex) {
            // Record a failed attempt
            attemptService.recordAttempt(ipAddress);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password. Please try again.");
        }
    }

    @Transactional
    public User register(UserRequest userRequest) {
        final String username = userRequest.getUsername();

        // 1) Brzi filter: ako kaže da SIGURNO NE postoji -> preskačemo DB check.
        //    Ako kaže "možda postoji" -> uradi potvrdu u bazi (tvoj postojeći check).
        if (usernameBloom.mightExist(username)) {
            User existingUser = userService.findByUsername(username);
            if (existingUser != null) {
                throw new ResourceConflictException(userRequest.getId(), "Username already exists");
            }
        }

        try {

            User saved = userService.save(userRequest);
            userService.flush(); // ⬅️ ostaje, proverava UNIQUE odmah (pre commit-a)

            // 3) Tek nakon uspešnog flush-a ubacujemo u Bloom da sledeći put bude brže.
            usernameBloom.add(username);

            return saved;


        } catch (DataIntegrityViolationException e) {

            throw new ResourceConflictException(null, "Username or email already exists");
        }
    }


    public boolean activateUser(String token) {
        return userService.activateUser(token);
    }
}
