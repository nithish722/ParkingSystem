package com.crisscross.ui.customer;

import com.crisscross.ui.ScreenManager;
import com.crisscross.ui.components.LiquidDockNavBar;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/**
 * Top Horizontal Navigation Bar for the Customer Portal.
 * Features the iOS 27 Liquid Glass Dock bar in Sunset Ember & Terracotta theme
 * with dynamic liquid follower pill, clean unicode glyphs, and zero-lag hardware transforms.
 */
public class CustomerHorizontalNav extends LiquidDockNavBar {

    public CustomerHorizontalNav(ScreenManager screenManager) {
        this(screenManager, "");
    }

    public CustomerHorizontalNav(ScreenManager screenManager, String activeNavId) {
        super(screenManager, activeNavId, "CUSTOMER PORTAL", "#F2A06A");

        addNavItem("📊  Dashboard", "DASHBOARD", () -> screenManager.showCustomerDashboard());
        addNavItem("📅  Campus Events", "EVENTS", () -> screenManager.showEventList());
        addNavItem("🎯  Smart Recommender", "RECOMMENDER", () -> screenManager.showSmartRecommendation(null, "CAR"));
        addNavItem("🧭  Parking Map", "MAP", () -> screenManager.showParkingMapView());
        addNavItem("🎫  My Reservations", "RESERVATIONS", () -> screenManager.showMyReservations());
        addNavItem("🔔  Announcements", "NOTIFICATIONS", () -> screenManager.showNotifications());
        addNavItem("👤  My Profile", "PROFILE", () -> screenManager.showProfile());
        addNavItem("💡  Help & About", "HELP", () -> screenManager.showHelpAndAbout());

        // Right side subtle edition badge
        Label editionLbl = new Label("CrissCross • Sunset Glow & Amber Gold Dock");
        editionLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8; -fx-font-weight: 600; -fx-padding: 0 4 0 0;");
        editionLbl.setMinWidth(Region.USE_PREF_SIZE);
        setRightNode(editionLbl);
    }
}
