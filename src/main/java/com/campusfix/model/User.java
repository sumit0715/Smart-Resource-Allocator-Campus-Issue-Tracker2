package com.campusfix.model;

import java.time.LocalDateTime;

public abstract class User {

    protected int userId;
    protected String name;
    protected String email;
    protected String password;
    protected LocalDateTime createdAt;

    protected User(int userId, String name, String email, String password) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.createdAt = LocalDateTime.now();
    }

    public abstract void showDashboard();

    public abstract Role getRole();

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s)", getRole(), name, email);
    }
}
