package com.crisscross.ui.customer;

import com.crisscross.ui.ScreenManager;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class HelpAndAboutView extends VBox {

    public HelpAndAboutView(ScreenManager screenManager) {
        this.setSpacing(22);
        this.setFillWidth(true);

        VBox header = new VBox(4);
        Label title = new Label("Help & System Documentation");
        title.getStyleClass().add("heading-1");
        Label subtitle = new Label("CrissCross Smart Event Parking Architecture, campus rules, and usage guide");
        subtitle.getStyleClass().add("text-subtle");
        header.getChildren().addAll(title, subtitle);

        // System Overview Card
        VBox aboutCard = new VBox(12);
        aboutCard.getStyleClass().add("glass-card");
        aboutCard.setPadding(new Insets(24));

        Label aboutTitle = new Label("About CrissCross");
        aboutTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #F2A06A;");

        Label aboutDesc = new Label("CrissCross is an enterprise-grade Java desktop course project engineered using pure JavaFX and MySQL. Designed with Apple-inspired frosted liquid glassmorphic UI aesthetics and a Warm Golden Amber & Vibrant Coral Rose theme (#E8B86D to #F16E68), it streamlines large-scale campus event parking by calculating real-time walking proximity and live bay capacity.");
        aboutDesc.setWrapText(true);
        aboutDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #CBD5E1; -fx-line-spacing: 4px;");

        aboutCard.getChildren().addAll(aboutTitle, aboutDesc);

        // Campus Parking Proximity Rules Card
        VBox rulesCard = new VBox(14);
        rulesCard.getStyleClass().add("glass-card-highlight");
        rulesCard.setPadding(new Insets(24));

        Label rulesTitle = new Label("CrissCross Proximity & Allocation Rules");
        rulesTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #F2A06A;");

        VBox r1 = createRuleRow("1. SJT Parking Hub", "Serving Silver Jubilee Tower, North Quad & CS Technology Block. Distance: ~50m (1-minute walk). Backup parking designated: CDMM Parking.");
        VBox r2 = createRuleRow("2. CDMM Parking Hub", "Serving Architecture Block, Design Studios & CDMM Main Hall. Distance: ~65m (1-minute walk). Backup parking designated: Woody's Parking.");
        VBox r3 = createRuleRow("3. Woody's Parking Grounds", "Serving Outdoor Sports Complex, Athletics Track & Food Court Arena. Distance: ~40m (1-minute walk). Backup parking designated: SJT Parking.");
        VBox r4 = createRuleRow("4. Vehicle Classification", "Strict separation of Four-Wheeler (Car) and Two-Wheeler (Bike) slots ensures efficient ingress and prevents gridlock during peak event conclusion.");

        rulesCard.getChildren().addAll(rulesTitle, r1, r2, r3, r4);

        // Technology Stack Card
        VBox techCard = new VBox(14);
        techCard.getStyleClass().add("glass-card");
        techCard.setPadding(new Insets(24));

        Label techTitle = new Label("Course Project Technical Specifications");
        techTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #F2A06A;");

        GridPane techGrid = new GridPane();
        techGrid.setHgap(28);
        techGrid.setVgap(12);

        techGrid.add(createSpecItem("Platform", "Windows Desktop Application (JavaFX)"), 0, 0);
        techGrid.add(createSpecItem("Language", "Java 21 / 25 LTS"), 1, 0);
        techGrid.add(createSpecItem("UI Framework", "JavaFX 21 Controls & FXML"), 0, 1);
        techGrid.add(createSpecItem("Styling", "JavaFX CSS Sunset Ember & Terracotta"), 1, 1);
        techGrid.add(createSpecItem("Database", "MySQL 8.x via JDBC Connector/J 8.3"), 0, 2);
        techGrid.add(createSpecItem("Architecture", "Service & DAO Multi-Layered Architecture"), 1, 2);

        techCard.getChildren().addAll(techTitle, techGrid);

        this.getChildren().addAll(header, aboutCard, rulesCard, techCard);
    }

    private VBox createRuleRow(String header, String detail) {
        VBox b = new VBox(2);
        Label h = new Label(header);
        h.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #fdf8f5;");
        Label d = new Label(detail);
        d.setWrapText(true);
        d.setStyle("-fx-font-size: 12px; -fx-text-fill: #CBD5E1;");
        b.getChildren().addAll(h, d);
        return b;
    }

    private VBox createSpecItem(String label, String value) {
        VBox b = new VBox(2);
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #F2A06A;");
        b.getChildren().addAll(l, v);
        return b;
    }
}
