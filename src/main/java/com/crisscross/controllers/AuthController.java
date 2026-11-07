package com.crisscross.controllers;

import com.crisscross.models.User;
import com.crisscross.services.AuthService;
import com.crisscross.ui.ScreenManager;

public class AuthController {
    private final AuthService authService = AuthService.getInstance();
    private final ScreenManager screenManager = ScreenManager.getInstance();

    public boolean handleLogin(String identifier, String password) throws Exception {
        User user = authService.login(identifier, password);
        if (user.isAdmin()) {
            screenManager.showAdminDashboard();
        } else {
            screenManager.showCustomerDashboard();
        }
        return true;
    }

    public boolean handleRegister(String fullName, String email, String username, String password,
                                  String confirmPassword, String phone, String vehicleNumber, String role) throws Exception {
        authService.register(fullName, email, username, password, confirmPassword, phone, vehicleNumber, role);
        screenManager.showCustomerDashboard();
        return true;
    }

    public boolean handleResetPassword(String emailOrUsername, String newPassword) throws Exception {
        return authService.resetPassword(emailOrUsername, newPassword);
    }

    public void handleLogout() {
        screenManager.showLogoutConfirmation();
    }
}
