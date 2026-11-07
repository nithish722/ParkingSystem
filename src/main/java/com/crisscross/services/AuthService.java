package com.crisscross.services;

import com.crisscross.dao.UserDAO;
import com.crisscross.models.User;

public class AuthService {
    private static AuthService instance;
    private final UserDAO userDAO;
    private User currentUser;

    private AuthService() {
        this.userDAO = new UserDAO();
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    public User login(String identifier, String password) throws Exception {
        if (identifier == null || identifier.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            throw new Exception("Please enter both username/email and password.");
        }

        User user = userDAO.findByUsernameOrEmail(identifier.trim());
        if (user == null) {
            throw new Exception("Account not found with provided identifier.");
        }

        if (!user.getPassword().equals(password)) {
            throw new Exception("Incorrect password. Please try again.");
        }

        if ("SUSPENDED".equalsIgnoreCase(user.getStatus())) {
            throw new Exception("This account has been suspended. Please contact administrator.");
        }

        this.currentUser = user;
        return user;
    }

    public User register(String fullName, String email, String username, String password,
                         String confirmPassword, String phone, String vehicleNumber, String role) throws Exception {
        if (fullName == null || fullName.trim().isEmpty()) throw new Exception("Full name is required.");
        if (email == null || !email.contains("@")) throw new Exception("Please provide a valid email address.");
        if (username == null || username.trim().length() < 3) throw new Exception("Username must be at least 3 characters.");
        validatePasswordConstraints(password, username);
        if (!password.equals(confirmPassword)) throw new Exception("Passwords do not match.");

        if (userDAO.isEmailOrUsernameTaken(email.trim(), username.trim(), -1)) {
            throw new Exception("Email or username is already registered.");
        }

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(email.trim());
        user.setUsername(username.trim());
        user.setPassword(password);
        user.setPhone(phone != null ? phone.trim() : "");
        user.setVehicleNumber(vehicleNumber != null ? vehicleNumber.trim().toUpperCase() : "");
        user.setRole(role != null ? role.toUpperCase() : "CUSTOMER");
        user.setStatus("ACTIVE");

        boolean ok = userDAO.insert(user);
        if (!ok) {
            throw new Exception("Failed to create user account. Please try again.");
        }

        this.currentUser = user;
        return user;
    }

    public boolean resetPassword(String emailOrUsername, String newPassword) throws Exception {
        if (emailOrUsername == null || emailOrUsername.trim().isEmpty()) {
            throw new Exception("Please enter your registered email or username.");
        }
        User user = userDAO.findByUsernameOrEmail(emailOrUsername.trim());
        if (user == null) {
            throw new Exception("No account found matching this email or username.");
        }

        validatePasswordConstraints(newPassword, user.getUsername());
        return userDAO.updatePassword(user.getUserId(), newPassword);
    }

    /**
     * Enforces strict account creation password criteria:
     * - Minimum 12 characters
     * - At least 1 uppercase letter
     * - At least 1 number
     * - At least 1 special character
     * - Must not contain username
     */
    public static void validatePasswordConstraints(String password, String username) throws Exception {
        if (password == null || password.length() < 12) {
            throw new Exception("Password must be at least 12 characters long.");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new Exception("Password must contain at least 1 uppercase letter (A-Z).");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new Exception("Password must contain at least 1 number (0-9).");
        }
        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            throw new Exception("Password must contain at least 1 special character (e.g. !@#$%^&*).");
        }
        if (username != null && !username.trim().isEmpty()) {
            String uLower = username.trim().toLowerCase();
            if (password.toLowerCase().contains(uLower)) {
                throw new Exception("Password cannot contain your username.");
            }
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public void logout() {
        this.currentUser = null;
    }
}
