package com.crisscross.ui.customer;

import com.crisscross.ui.ScreenManager;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class SplashScreenView extends StackPane {

    public SplashScreenView(ScreenManager screenManager) {
        this.setAlignment(Pos.CENTER);

        VBox glassCard = new VBox(22);
        glassCard.getStyleClass().add("glass-card");
        glassCard.setMaxWidth(520);
        glassCard.setAlignment(Pos.CENTER);
        glassCard.setStyle("-fx-padding: 48px 40px;");

        // Glowing App Symbol
        Label icon = new Label("⛶");
        icon.setStyle("-fx-font-size: 72px; -fx-text-fill: #F2A06A;");
        DropShadow glow = new DropShadow(35, Color.web("#F16E68", 0.65));
        icon.setEffect(glow);

        Label title = new Label("CrissCross");
        title.getStyleClass().add("heading-hero");
        title.setStyle("-fx-font-size: 38px; -fx-text-fill: #F2A06A;");

        Label subtitle = new Label("Smart Event Parking Management System");
        subtitle.setStyle("-fx-font-size: 15px; -fx-text-fill: #CBD5E1; -fx-font-weight: 500;");

        Label techPill = new Label("Sunset Glow & Golden Amber • Pure JavaFX • Liquid Glassmorphism");
        techPill.setStyle("-fx-background-color: rgba(242, 160, 106, 0.16); -fx-text-fill: #F2A06A; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 6px 16px; -fx-background-radius: 14px; -fx-border-color: rgba(242, 160, 106, 0.40); -fx-border-radius: 14px;");

        // Loading Bar
        ProgressBar progressBar = new ProgressBar(0.0);
        progressBar.setPrefWidth(320);
        progressBar.setPrefHeight(6);
        progressBar.setStyle("-fx-accent: #F16E68;");

        Label statusLbl = new Label("Initializing components & database connectivity...");
        statusLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8;");

        glassCard.getChildren().addAll(icon, title, subtitle, techPill, progressBar, statusLbl);
        this.getChildren().add(glassCard);

        // Smooth Animation Timeline
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressBar.progressProperty(), 0.0)),
                new KeyFrame(Duration.millis(800), e -> statusLbl.setText("Loading SJT, CDMM & Woody's parking bays...")),
                new KeyFrame(Duration.millis(1600), e -> statusLbl.setText("Calibrating proximity recommendation rules...")),
                new KeyFrame(Duration.millis(2400), new KeyValue(progressBar.progressProperty(), 1.0)),
                new KeyFrame(Duration.millis(2600), e -> screenManager.showWelcome())
        );
        timeline.play();
    }
}
