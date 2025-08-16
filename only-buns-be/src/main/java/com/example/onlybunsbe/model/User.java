package com.example.onlybunsbe.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 3, max = 100)
    @Column(name = "username", unique = true)
    private String username;

    @JsonIgnore
    @NotNull
    @Size(min = 8, max = 255)
    @Column(name = "password")
    private String password;

    @NotNull
    @Email
    @Column(name = "email", unique = true)
    private String email;

    @NotNull
    @Size(max = 100)
    @Column(name = "first_name")
    private String firstName;

    @NotNull
    @Size(max = 100)
    @Column(name = "last_name")
    private String lastName;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "enabled")
    private boolean enabled = false;

    @JsonIgnore
    @Column(name = "last_password_reset_date")
    private Timestamp lastPasswordResetDate;

    @JsonIgnore
    private Instant lastLoginDate;

    @JsonIgnore
    @Column(name = "activation_token")
    private String activationToken;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Post> posts;

    @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Follow> following = new HashSet<>();

    @OneToMany(mappedBy = "followed", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Follow> followers = new HashSet<>();

    @JsonIgnore @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(role); }
    @JsonIgnore @Override public boolean isAccountNonExpired() { return true; }
    @JsonIgnore @Override public boolean isAccountNonLocked() { return true; }
    @JsonIgnore @Override public boolean isCredentialsNonExpired() { return true; }

    public void setPassword(String password) {
        this.password = password;
        this.lastPasswordResetDate = new Timestamp(new Date().getTime());
    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GroupChatMember> groupMemberships = new HashSet<>();

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ChatMessage> sentMessages;
}
