package com.crisscross.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private int userId;
    private String fullName;
    private String email;
    private String username;
    private String password;
    private String phone;
    private String vehicleNumber;
    private String role; // "CUSTOMER" or "ADMIN"
    private String status; // "ACTIVE", "SUSPENDED"
    private LocalDateTime createdAt;

    public User() {
        this.role = "CUSTOMER";
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public User(int userId, String fullName, String email, String username, String password,
                String phone, String vehicleNumber, String role, String status) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.username = username;
        this.password = password;
        this.phone = phone;
        this.vehicleNumber = vehicleNumber;
        this.role = role != null ? role : "CUSTOMER";
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    @Override
    public String toString() {
        return fullName + " (" + username + " - " + role + ")";
    }
}
