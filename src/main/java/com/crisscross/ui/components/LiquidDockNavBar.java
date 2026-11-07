package com.crisscross.ui.components;

import com.crisscross.ui.ScreenManager;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * iOS 27 Liquid Glass Dock Navigation Bar.
 * Features an authentic Apple dock bar with dynamic liquid glass pill follower,
 * silky smooth 60 FPS spring glide, tactile click bounce,
 * zero horizontal scrollbar clipping, and zero-lag hardware-accelerated transforms.
 */
public class LiquidDockNavBar extends HBox {
    protected final ScreenManager screenManager;
    protected final String activeNavId;

    private final List<Button> navButtons = new ArrayList<>();
    private final HBox navButtonsBox = new HBox(6);
    private final Pane dockPane = new Pane();
    private final Region liquidPill = new Region();

    private final DoubleProperty pillX = new SimpleDoubleProperty(0);
    private final DoubleProperty pillW = new SimpleDoubleProperty(0);
    private Timeline pillTimeline = null;

    private Button activeButton = null;

    public LiquidDockNavBar(ScreenManager screenManager, String activeNavId, String badgeTitle, String badgeColor) {
        this.screenManager = screenManager;
        this.activeNavId = activeNavId != null ? activeNavId : "";

        this.getStyleClass().add("glass-topnav");
        this.setAlignment(Pos.CENTER_LEFT);
        this.setSpacing(12);
        this.setPadding(new Insets(6, 18, 6, 18));

        // 1. Portal Badge on Left (Fixed min-width so never truncated into "CUSTOMER PO...")
        if (badgeTitle != null && !badgeTitle.isEmpty()) {
            Label badge = new Label(badgeTitle);
            badge.setMinWidth(Region.USE_PREF_SIZE);
            badge.setMaxWidth(Region.USE_PREF_SIZE);
            badge.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: " + badgeColor + "; "
                    + "-fx-background-color: rgba(242, 160, 106, 0.16); -fx-padding: 6px 12px; "
                    + "-fx-background-radius: 14px; -fx-border-color: rgba(242, 160, 106, 0.35); -fx-border-radius: 14px;");
            this.getChildren().add(badge);
        }

        // 2. Setup the Liquid Glass Dock Track
        setupDockTrack();

        // 3. ScrollPane wrapper for responsiveness - NEVER show horizontal/vertical scrollbars!
        ScrollPane scrollWrapper = new ScrollPane(dockPane);
        scrollWrapper.getStyleClass().add("dock-scroll-wrapper");
        scrollWrapper.setFitToHeight(true);
        scrollWrapper.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollWrapper.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        HBox.setHgrow(scrollWrapper, Priority.ALWAYS);

        this.getChildren().add(scrollWrapper);
    }

    private void setupDockTrack() {
        navButtonsBox.setAlignment(Pos.CENTER_LEFT);
        navButtonsBox.setPadding(new Insets(2, 6, 2, 6));

        // Liquid Pill follower (lives behind buttons in dockPane)
        liquidPill.setMouseTransparent(true);
        liquidPill.getStyleClass().add("liquid-dock-pill");
        liquidPill.setManaged(false);
        liquidPill.setOpacity(0.0);
        liquidPill.setCache(true);
        liquidPill.setCacheHint(CacheHint.SPEED);

        // Bind animated properties to unmanaged pill position & size
        pillX.addListener((obs, oldV, newV) -> liquidPill.setLayoutX(newV.doubleValue()));
        pillW.addListener((obs, oldV, newV) -> {
            double h = liquidPill.getHeight() > 0 ? liquidPill.getHeight() : 36.0;
            liquidPill.resize(newV.doubleValue(), h);
        });

        dockPane.getChildren().addAll(liquidPill, navButtonsBox);
        dockPane.prefWidthProperty().bind(navButtonsBox.widthProperty());
        dockPane.prefHeightProperty().bind(navButtonsBox.heightProperty());
        dockPane.minWidthProperty().bind(navButtonsBox.widthProperty());
        dockPane.minHeightProperty().bind(navButtonsBox.heightProperty());

        // Mouse leaves entire dock track -> smooth glide back to active button
        navButtonsBox.setOnMouseExited(e -> {
            if (activeButton != null) {
                animatePillTo(activeButton);
            } else {
                fadeOutPill();
            }
        });

        // Automatically position pill on first layout pass
        navButtonsBox.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(this::syncPillToActiveImmediate);
        });
    }

    public void addNavItem(String text, String navId, Runnable action) {
        Button btn = new Button(text);
        btn.setMinWidth(Region.USE_PREF_SIZE);
        boolean isActive = activeNavId != null && activeNavId.equalsIgnoreCase(navId);

        btn.getStyleClass().add("top-nav-button");
        if (isActive) {
            btn.getStyleClass().add("top-nav-button-active");
            this.activeButton = btn;
        }

        // Silky smooth, lag-free hover glide & subtle lift
        btn.setOnMouseEntered(e -> {
            animatePillTo(btn);
            TranslateTransition tt = new TranslateTransition(Duration.millis(120), btn);
            tt.setToY(-2.0);
            tt.setInterpolator(Interpolator.EASE_OUT);
            tt.play();
        });

        btn.setOnMouseExited(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(120), btn);
            tt.setToY(0.0);
            tt.setInterpolator(Interpolator.EASE_OUT);
            tt.play();
        });

        // Tactile liquid spring bounce on click
        btn.setOnAction(e -> {
            playClickBounce(btn);
            action.run();
        });

        navButtons.add(btn);
        navButtonsBox.getChildren().add(btn);
    }

    public void setRightNode(Node rightNode) {
        if (rightNode != null) {
            if (rightNode instanceof Region) {
                ((Region) rightNode).setMinWidth(Region.USE_PREF_SIZE);
            }
            this.getChildren().add(rightNode);
        }
    }

    private void animatePillTo(Button targetBtn) {
        if (targetBtn == null || targetBtn.getWidth() <= 0) return;

        double targetX = targetBtn.getBoundsInParent().getMinX();
        double targetY = targetBtn.getBoundsInParent().getMinY();
        double targetW = targetBtn.getBoundsInParent().getWidth();
        double targetH = targetBtn.getBoundsInParent().getHeight();

        liquidPill.setLayoutY(targetY);
        if (liquidPill.getHeight() != targetH && targetH > 0) {
            liquidPill.resize(liquidPill.getWidth(), targetH);
        }

        if (pillTimeline != null) {
            pillTimeline.stop();
        }

        // Apple spring cubic bezier curve
        Interpolator springEase = Interpolator.SPLINE(0.18, 0.95, 0.25, 1.0);

        pillTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(pillX, liquidPill.getLayoutX()),
                        new KeyValue(pillW, liquidPill.getWidth()),
                        new KeyValue(liquidPill.opacityProperty(), liquidPill.getOpacity())
                ),
                new KeyFrame(Duration.millis(180),
                        new KeyValue(pillX, targetX, springEase),
                        new KeyValue(pillW, targetW, springEase),
                        new KeyValue(liquidPill.opacityProperty(), 1.0, springEase)
                )
        );
        pillTimeline.play();
    }

    private void syncPillToActiveImmediate() {
        if (activeButton != null && activeButton.getWidth() > 0) {
            double x = activeButton.getBoundsInParent().getMinX();
            double y = activeButton.getBoundsInParent().getMinY();
            double w = activeButton.getBoundsInParent().getWidth();
            double h = activeButton.getBoundsInParent().getHeight();

            pillX.set(x);
            pillW.set(w);
            liquidPill.setLayoutX(x);
            liquidPill.setLayoutY(y);
            liquidPill.resize(w, h > 0 ? h : 36.0);
            liquidPill.setOpacity(1.0);
        } else if (activeButton == null) {
            liquidPill.setOpacity(0.0);
        }
    }

    private void fadeOutPill() {
        if (pillTimeline != null) pillTimeline.stop();
        pillTimeline = new Timeline(
                new KeyFrame(Duration.millis(160),
                        new KeyValue(liquidPill.opacityProperty(), 0.0, Interpolator.EASE_BOTH)
                )
        );
        pillTimeline.play();
    }

    private void playClickBounce(Button btn) {
        ScaleTransition st = new ScaleTransition(Duration.millis(90), btn);
        st.setFromX(btn.getScaleX());
        st.setFromY(btn.getScaleY());
        st.setToX(0.94);
        st.setToY(0.94);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();
    }
}
