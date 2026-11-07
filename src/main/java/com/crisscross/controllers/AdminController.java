package com.crisscross.controllers;

import com.crisscross.dao.NotificationDAO;
import com.crisscross.dao.UserDAO;
import com.crisscross.models.Notification;
import com.crisscross.models.User;
import com.crisscross.services.ReportService;

import java.util.List;

public class AdminController {
    private final ReportService reportService = ReportService.getInstance();
    private final UserDAO userDAO = new UserDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    public ReportService.SystemStats getSystemStatistics() {
        return reportService.getSystemStats();
    }

    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    public boolean toggleUserStatus(int userId) {
        User u = userDAO.findById(userId);
        if (u != null) {
            String newStatus = "ACTIVE".equalsIgnoreCase(u.getStatus()) ? "SUSPENDED" : "ACTIVE";
            u.setStatus(newStatus);
            return userDAO.update(u);
        }
        return false;
    }

    public boolean broadcastNotification(String title, String message, String priority, String targetRole) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setMessage(message);
        n.setPriority(priority);
        n.setTargetRole(targetRole);
        return notificationDAO.insert(n);
    }
}
