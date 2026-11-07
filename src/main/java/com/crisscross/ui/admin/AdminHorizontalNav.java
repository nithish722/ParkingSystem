package com.crisscross.ui.admin;

import com.crisscross.ui.ScreenManager;
import com.crisscross.ui.components.LiquidDockNavBar;
import javafx.scene.control.Button;

/**
 * Top Horizontal Navigation Bar for the Admin Console.
 * Features the iOS 27 Liquid Glass Dock bar in Sunset Ember & Terracotta theme
 * with dynamic liquid follower pill, clean unicode glyphs, and zero-lag hardware transforms.
 */
public class AdminHorizontalNav extends LiquidDockNavBar {

    public AdminHorizontalNav(ScreenManager screenManager) {
        this(screenManager, "");
    }

    public AdminHorizontalNav(ScreenManager screenManager, String activeNavId) {
        super(screenManager, activeNavId, "ADMIN CONSOLE", "#F2A06A");

        addNavItem("📊  Dashboard", "ADMIN_DASHBOARD", () -> screenManager.showAdminDashboard());
        addNavItem("📅  Events", "ADMIN_EVENTS", () -> screenManager.showAdminEvents());
        addNavItem("🏢  Areas", "ADMIN_AREAS", () -> screenManager.showAdminParkingAreas());
        addNavItem("🔢  Slots", "ADMIN_SLOTS", () -> screenManager.showAdminSlots());
        addNavItem("🚦  Status Monitor", "ADMIN_STATUS", () -> screenManager.showAdminParkingStatus());
        addNavItem("📋  Bookings", "ADMIN_RESERVATIONS", () -> screenManager.showAdminReservations());
        addNavItem("👥  Users", "ADMIN_USERS", () -> screenManager.showAdminUsers());
        addNavItem("📢  Announcements", "ADMIN_NOTIFICATIONS", () -> screenManager.showAdminNotifications());
        addNavItem("📈  Reports", "ADMIN_REPORTS", () -> screenManager.showAdminReports());
        addNavItem("👤  Profile", "ADMIN_PROFILE", () -> screenManager.showAdminProfile());

        Button switchCustomerBtn = new Button("Switch to Customer View");
        switchCustomerBtn.getStyleClass().add("button-secondary");
        switchCustomerBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6px 14px;");
        switchCustomerBtn.setOnAction(e -> screenManager.showCustomerDashboard());
        setRightNode(switchCustomerBtn);
    }
}
