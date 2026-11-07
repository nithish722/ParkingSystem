package com.crisscross.ui;

import com.crisscross.models.*;
import com.crisscross.services.AuthService;
import com.crisscross.ui.admin.*;
import com.crisscross.ui.components.GlassHeader;
import com.crisscross.ui.customer.*;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Stack;

/**
 * Central UI Router and Screen Lifecycle Manager for CrissCross.
 * Handles Apple-inspired glassmorphic transitions, light/dark theme switching,
 * navigation history, and global session state.
 */
public class ScreenManager {
    private static ScreenManager instance;
    private Stage primaryStage;
    private Scene mainScene;
    private StackPane rootContainer;

    // Theme state (Default to original Apple-inspired Dark Glass theme)
    private boolean isDarkMode = true;
    private final String DARK_CSS = getClass().getResource("/css/glass-dark.css").toExternalForm();
    private final String LIGHT_CSS = getClass().getResource("/css/glass-light.css").toExternalForm();

    // Navigation history
    private final Stack<Runnable> navigationHistory = new Stack<>();

    // Active Booking Workflow State
    private Event selectedEventForBooking;
    private String selectedVehicleType = "CAR";
    private String userVehicleNumber = "";
    private ParkingArea selectedParkingArea;
    private ParkingSlot selectedParkingSlot;
    private Reservation lastConfirmedReservation;

    private ScreenManager() {}

    public static synchronized ScreenManager getInstance() {
        if (instance == null) {
            instance = new ScreenManager();
        }
        return instance;
    }

    public void init(Stage stage) {
        this.primaryStage = stage;
        this.rootContainer = new StackPane();
        this.mainScene = new Scene(rootContainer, 1280, 820);

        applyTheme();

        primaryStage.setTitle("CrissCross - Smart Event Parking Management System");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(720);
        primaryStage.setScene(mainScene);
        primaryStage.show();

        // Start with Splash Screen
        showSplash();
    }

    public void applyTheme() {
        mainScene.getStylesheets().clear();
        if (isDarkMode) {
            mainScene.getStylesheets().add(DARK_CSS);
        } else {
            mainScene.getStylesheets().add(LIGHT_CSS);
        }
    }

    public void toggleTheme() {
        isDarkMode = !isDarkMode;
        applyTheme();
    }

    public boolean isDarkMode() {
        return isDarkMode;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    // --- Transition Helper ---
    public void setContent(Node newContent) {
        newContent.setOpacity(0.0);
        rootContainer.getChildren().setAll(newContent);
        FadeTransition ft = new FadeTransition(Duration.millis(280), newContent);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    // --- Navigation Wrappers ---
    public void showAppShell(Node viewContent, boolean isAdmin) {
        showAppShell(viewContent, isAdmin, "");
    }

    public void showAppShell(Node viewContent, boolean isAdmin, String activeNavId) {
        BorderPane shell = new BorderPane();

        // Top horizontal panel containing GlassHeader + Top Horizontal Navigation Bar
        VBox topPanel = new VBox();
        topPanel.getChildren().add(new GlassHeader(this));

        Node topNavBar = isAdmin ? new AdminHorizontalNav(this, activeNavId) : new CustomerHorizontalNav(this, activeNavId);
        topPanel.getChildren().add(topNavBar);

        shell.setTop(topPanel);
        shell.setLeft(null); // Menu is now at the top horizontal panel, not in the vertical panel

        // Content wrapped in glass scrollpane
        ScrollPane sp = new ScrollPane(viewContent);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("scroll-pane");
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox contentBox = new VBox(sp);
        contentBox.setPadding(new Insets(20, 28, 28, 28));
        VBox.setVgrow(sp, Priority.ALWAYS);

        shell.setCenter(contentBox);
        setContent(shell);
    }

    // --- Screen Launchers ---
    public void showSplash() {
        setContent(new SplashScreenView(this));
    }

    public void showWelcome() {
        navigationHistory.clear();
        setContent(new WelcomeScreenView(this));
    }

    public void showLogin() {
        setContent(new LoginView(this));
    }

    public void showRegister() {
        setContent(new RegistrationView(this));
    }

    public void showForgotPassword() {
        setContent(new ForgotPasswordView(this));
    }

    // Customer Screens
    public void showCustomerDashboard() {
        showAppShell(new CustomerDashboardView(this), false, "DASHBOARD");
    }

    public void showEventList() {
        showAppShell(new EventListView(this), false, "EVENTS");
    }

    public void showEventDetails(Event event) {
        this.selectedEventForBooking = event;
        showAppShell(new EventDetailsView(this, event), false, "EVENTS");
    }

    public void showVehicleSelection(Event event) {
        this.selectedEventForBooking = event;
        showAppShell(new VehicleSelectionView(this, event), false, "EVENTS");
    }

    public void showParkingAvailability(Event event, String vehicleType, String vehicleNumber) {
        this.selectedEventForBooking = event;
        this.selectedVehicleType = vehicleType;
        this.userVehicleNumber = vehicleNumber;
        showAppShell(new ParkingAvailabilityView(this, event, vehicleType, vehicleNumber), false, "EVENTS");
    }

    public void showSmartRecommendation(Event event, String vehicleType) {
        this.selectedEventForBooking = event;
        this.selectedVehicleType = vehicleType;
        showAppShell(new SmartRecommendationView(this, event, vehicleType), false, "RECOMMENDER");
    }

    public void showParkingAreaDetails(ParkingArea area) {
        this.selectedParkingArea = area;
        showAppShell(new ParkingAreaDetailsView(this, area), false, "MAP");
    }

    public void showParkingMapView() {
        showAppShell(new ParkingMapView(this), false, "MAP");
    }

    public void showSlotSelection(Event event, ParkingArea area, String vehicleType) {
        this.selectedEventForBooking = event;
        this.selectedParkingArea = area;
        this.selectedVehicleType = vehicleType;
        showAppShell(new SlotSelectionView(this, event, area, vehicleType), false, "EVENTS");
    }

    public void showBookingSummary(Event event, ParkingArea area, ParkingSlot slot, String vehicleType, String vehicleNumber) {
        this.selectedEventForBooking = event;
        this.selectedParkingArea = area;
        this.selectedParkingSlot = slot;
        this.selectedVehicleType = vehicleType;
        this.userVehicleNumber = vehicleNumber;
        showAppShell(new BookingSummaryView(this, event, area, slot, vehicleType, vehicleNumber), false, "EVENTS");
    }

    public void showBookingConfirmation(Reservation reservation) {
        this.lastConfirmedReservation = reservation;
        showAppShell(new BookingConfirmationView(this, reservation), false, "RESERVATIONS");
    }

    public void showMyReservations() {
        showAppShell(new MyReservationsView(this), false, "RESERVATIONS");
    }

    public void showReservationDetails(Reservation reservation) {
        showAppShell(new ReservationDetailsView(this, reservation), false, "RESERVATIONS");
    }

    public void showProfile() {
        showAppShell(new ProfileView(this), false, "PROFILE");
    }

    public void showNotifications() {
        showAppShell(new NotificationsView(this), false, "NOTIFICATIONS");
    }

    public void showHelpAndAbout() {
        showAppShell(new HelpAndAboutView(this), false, "HELP");
    }

    // Admin Screens
    public void showAdminDashboard() {
        showAppShell(new AdminDashboardView(this), true, "ADMIN_DASHBOARD");
    }

    public void showAdminEvents() {
        showAppShell(new EventManagementView(this), true, "ADMIN_EVENTS");
    }

    public void showAdminParkingAreas() {
        showAppShell(new ParkingAreaManagementView(this), true, "ADMIN_AREAS");
    }

    public void showAdminSlots() {
        showAppShell(new SlotManagementView(this), true, "ADMIN_SLOTS");
    }

    public void showAdminParkingStatus() {
        showAppShell(new ParkingStatusManagementView(this), true, "ADMIN_STATUS");
    }

    public void showAdminReservations() {
        showAppShell(new ReservationManagementView(this), true, "ADMIN_RESERVATIONS");
    }

    public void showAdminUsers() {
        showAppShell(new UserManagementView(this), true, "ADMIN_USERS");
    }

    public void showAdminNotifications() {
        showAppShell(new NotificationManagementView(this), true, "ADMIN_NOTIFICATIONS");
    }

    public void showAdminReports() {
        showAppShell(new ReportsDashboardView(this), true, "ADMIN_REPORTS");
    }

    public void showAdminProfile() {
        showAppShell(new AdminProfileView(this), true, "ADMIN_PROFILE");
    }

    // Modal Confirmation Dialog
    public void showConfirmationDialog(String title, String message, Runnable onConfirm) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("modal-overlay");

        VBox dialog = new VBox(20);
        dialog.getStyleClass().add("modal-dialog");
        dialog.setMaxWidth(450);
        dialog.setAlignment(Pos.CENTER);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("heading-1");

        Label msgLbl = new Label(message);
        msgLbl.setWrapText(true);
        msgLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-text-alignment: center;");

        HBox btnBox = new HBox(16);
        btnBox.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("button-secondary");
        cancelBtn.setOnAction(e -> rootContainer.getChildren().remove(overlay));

        Button confirmBtn = new Button("Confirm");
        confirmBtn.getStyleClass().add("button-primary");
        confirmBtn.setOnAction(e -> {
            rootContainer.getChildren().remove(overlay);
            onConfirm.run();
        });

        btnBox.getChildren().addAll(cancelBtn, confirmBtn);
        dialog.getChildren().addAll(titleLbl, msgLbl, btnBox);
        overlay.getChildren().add(dialog);

        rootContainer.getChildren().add(overlay);
    }

    public void showCustomModal(Node modalOverlay) {
        if (rootContainer != null && !rootContainer.getChildren().contains(modalOverlay)) {
            modalOverlay.setOpacity(0.0);
            rootContainer.getChildren().add(modalOverlay);
            FadeTransition ft = new FadeTransition(Duration.millis(220), modalOverlay);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }
    }

    public void closeCustomModal(Node modalOverlay) {
        if (rootContainer != null) {
            rootContainer.getChildren().remove(modalOverlay);
        }
    }

    public void showLogoutConfirmation() {
        showConfirmationDialog("Confirm Logout", "Are you sure you want to end your CrissCross session?", () -> {
            AuthService.getInstance().logout();
            showLogin();
        });
    }

    // Active state getters & setters
    public Event getSelectedEventForBooking() { return selectedEventForBooking; }
    public void setSelectedEventForBooking(Event event) { this.selectedEventForBooking = event; }
    public String getSelectedVehicleType() { return selectedVehicleType; }
    public void setSelectedVehicleType(String type) { this.selectedVehicleType = type; }
    public String getUserVehicleNumber() { return userVehicleNumber; }
    public void setUserVehicleNumber(String num) { this.userVehicleNumber = num; }
    public ParkingArea getSelectedParkingArea() { return selectedParkingArea; }
    public void setSelectedParkingArea(ParkingArea area) { this.selectedParkingArea = area; }
    public ParkingSlot getSelectedParkingSlot() { return selectedParkingSlot; }
    public void setSelectedParkingSlot(ParkingSlot slot) { this.selectedParkingSlot = slot; }
    public Reservation getLastConfirmedReservation() { return lastConfirmedReservation; }
}
