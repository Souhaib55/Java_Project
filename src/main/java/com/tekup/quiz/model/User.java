package com.tekup.quiz.model;

import java.util.Objects;

public class User {
    private long id;
    private String username;
    private String passwordHash;
    private Role role;

    public User(long id, String username, String passwordHash, Role role) {
        this.id = id;
        this.username = validateUsername(username);
        this.passwordHash = validatePasswordHash(passwordHash);
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    public User(String username, String passwordHash, Role role) {
        this(0L, username, passwordHash, role);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = validateUsername(username);
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = validatePasswordHash(passwordHash);
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    private String validateUsername(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        return value.trim();
    }

    private String validatePasswordHash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Password hash must not be blank");
        }
        return value;
    }
}
