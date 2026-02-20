package com.accrevent.radius.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_logout")
public class UserLogout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;  // Keycloak user ID

    @Column(nullable = false)
    private Instant logoutTime;

    public UserLogout() {}

    public UserLogout(String userId, Instant logoutTime) {
        this.userId = userId;
        this.logoutTime = logoutTime;
    }

    // Getters and setters
    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public Instant getLogoutTime() { return logoutTime; }
    public void setId(Long id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setLogoutTime(Instant logoutTime) { this.logoutTime = logoutTime; }
}

