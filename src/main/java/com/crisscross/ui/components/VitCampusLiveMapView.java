package com.crisscross.ui.components;

import com.crisscross.dao.ParkingAreaDAO;
import com.crisscross.models.Event;
import com.crisscross.models.ParkingArea;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URL;
import java.util.function.Consumer;

/**
 * Real-Time Interactive Live Geographic Map View of VIT Vellore Campus.
 * Powered by JavaFX WebView + Leaflet.js with live OpenStreetMap, CartoDB Voyager,
 * and Esri World Satellite imagery tiles.
 * 
 * Features:
 * - Live real-world map tiles of VIT Vellore campus (12.9692° N, 79.1559° E)
 * - Multiple tile layer modes: Clean Glass Voyager, Street Walkways, Satellite
 * - Interactive parking lot markers (SJT, CDMM, Woody's) with live real-time capacities
 * - Real-time click-to-pin anywhere on campus with Haversine walking distance calculation
 * - Dynamic event venue highlighting & proximity indicators
 * - Responsive full-height scaling for 100% full visibility
 */
public class VitCampusLiveMapView extends VBox {

    private final ParkingAreaDAO areaDAO = new ParkingAreaDAO();
    private Consumer<ParkingArea> onAreaSelectedListener;
    private ParkingArea currentSelectedArea;
    private Event activeEvent;

    // WebView & WebEngine
    private final WebView webView = new WebView();
    private final WebEngine webEngine = webView.getEngine();
    private final StackPane webContainer = new StackPane();
    private final ProgressIndicator loadingSpinner = new ProgressIndicator();

    // Status & Telemetry UI
    private final Label statusLabel = new Label("● Live VIT Vellore Campus Map • Initializing tile engine...");
    private final Label selectionSummaryLabel = new Label("Select a parking lot on the live map or click campus to drop a pin");
    private final Button confirmDestinationBtn = new Button("Select Destination →");

    // Tile mode tracking
    private String currentTileMode = "voyager";

    public VitCampusLiveMapView() {
        this(null, null);
    }

    public VitCampusLiveMapView(Event event, Consumer<ParkingArea> onAreaSelected) {
        this.activeEvent = event;
        this.onAreaSelectedListener = onAreaSelected;

        this.setSpacing(10);
        this.setFillWidth(true);
        this.setPadding(new Insets(12));
        this.getStyleClass().add("glass-card");
        this.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, rgba(255, 255, 255, 0.05) 0%, rgba(16, 36, 24, 0.16) 50%, rgba(5, 14, 9, 0.24) 100%); -fx-background-radius: 24px; -fx-border-color: rgba(255, 255, 255, 0.36) rgba(52, 211, 153, 0.45) rgba(52, 211, 153, 0.15) rgba(255, 255, 255, 0.24); -fx-border-radius: 24px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.25), 24, 0.08, 0, 8);");

        // 1. Top Controls Bar (Layer switchers, quick navigation presets, zoom)
        HBox topBar = createTopToolbar();

        // 2. Center Web Viewport (Pannable & Zoomable Live Geographic Slippy Map)
        setupWebMap();

        // 3. Bottom Telemetry & Status Strip
        HBox bottomStrip = createBottomStrip();

        this.getChildren().addAll(topBar, webContainer, bottomStrip);
        VBox.setVgrow(this, Priority.ALWAYS);

        // Default to SJT selection
        ParkingArea defaultArea = areaDAO.findByCode("SJT");
        if (defaultArea != null) {
            this.currentSelectedArea = defaultArea;
            updateSummaryDisplay(defaultArea);
        }
    }

    public void setOnAreaSelected(Consumer<ParkingArea> listener) {
        this.onAreaSelectedListener = listener;
    }

    public void setActiveEvent(Event event) {
        this.activeEvent = event;
        if (event != null && event.getVenue() != null) {
            runScript(String.format("if (window.highlightEventVenue) { window.highlightEventVenue('%s'); }", escapeJs(event.getVenue())));
        }
    }

    public void selectArea(String code) {
        ParkingArea area = areaDAO.findByCode(code);
        if (area == null) return;
        this.currentSelectedArea = area;

        updateSummaryDisplay(area);

        // Instruct Leaflet map to pan/zoom to this area
        runScript(String.format("if (window.focusArea) { window.focusArea('%s'); }", code));

        if (onAreaSelectedListener != null) {
            onAreaSelectedListener.accept(area);
        }
    }

    private void updateSummaryDisplay(ParkingArea area) {
        selectionSummaryLabel.setText(String.format("Destination: %s (%s) • %d/%d Slots Free • %s (%d min walk)",
                area.getName(), area.getCode(), area.getAvailableCapacity(), area.getTotalCapacity(),
                area.getNearestVenue(), area.getWalkingTimeMins()));
        confirmDestinationBtn.setText("Confirm " + area.getCode() + " as Destination & Pick Slot →");
        confirmDestinationBtn.setDisable(false);

        statusLabel.setText("● Selected Destination: " + area.getName() + " (" + area.getAvailableCapacity() + " Free)");
    }

    private void setupWebMap() {
        webContainer.setMinHeight(500);
        webContainer.setPrefHeight(600);
        VBox.setVgrow(webContainer, Priority.ALWAYS);
        webContainer.setStyle("-fx-background-color: rgba(4, 10, 6, 0.75); -fx-background-radius: 18px; -fx-border-color: rgba(52, 211, 153, 0.35); -fx-border-radius: 18px;");

        // Clip container with smooth rounded corners
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(webContainer.widthProperty());
        clip.heightProperty().bind(webContainer.heightProperty());
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        webContainer.setClip(clip);

        // Bind WebView size to container
        webView.prefWidthProperty().bind(webContainer.widthProperty());
        webView.prefHeightProperty().bind(webContainer.heightProperty());

        loadingSpinner.setMaxSize(48, 48);
        webContainer.getChildren().addAll(webView, loadingSpinner);

        // Configure WebEngine
        webEngine.setJavaScriptEnabled(true);

        // Handle page load completion and register Java-JS bridge
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                loadingSpinner.setVisible(false);
                try {
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("javaBridge", new MapBridge());

                    syncCapacitiesFromDAO();

                    if (activeEvent != null && activeEvent.getVenue() != null) {
                        runScript(String.format("if (window.highlightEventVenue) { window.highlightEventVenue('%s'); }", escapeJs(activeEvent.getVenue())));
                    }

                    if (currentSelectedArea != null) {
                        runScript(String.format("if (window.focusArea) { window.focusArea('%s'); }", currentSelectedArea.getCode()));
                    }

                    statusLabel.setText("● Live VIT Vellore Campus Map • Real Tile Stream Active");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else if (newState == Worker.State.FAILED) {
                loadingSpinner.setVisible(false);
                statusLabel.setText("⚠️ Failed to load map tiles. Please check internet connection.");
            }
        });

        // Trigger map invalidateSize on container resize so it fills 100% of the visible space
        webContainer.widthProperty().addListener((obs, oldW, newW) -> runScript("if (window.map) { window.map.invalidateSize(); }"));
        webContainer.heightProperty().addListener((obs, oldH, newH) -> runScript("if (window.map) { window.map.invalidateSize(); }"));

        // Load the HTML map file from resources
        URL mapUrl = getClass().getResource("/html/vit_live_map.html");
        if (mapUrl != null) {
            webEngine.load(mapUrl.toExternalForm());
        }
    }

    private void syncCapacitiesFromDAO() {
        for (String code : new String[]{"SJT", "CDMM", "WOODYS"}) {
            ParkingArea area = areaDAO.findByCode(code);
            if (area != null) {
                runScript(String.format("if (window.updateSlotCapacity) { window.updateSlotCapacity('%s', %d, %d); }",
                        code, area.getAvailableCapacity(), area.getTotalCapacity()));
            }
        }
    }

    private HBox createTopToolbar() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(2, 4, 4, 4));

        Label title = new Label("📍 VIT Vellore Live Map:");
        title.setStyle("-fx-text-fill: #34d399; -fx-font-size: 12px; -fx-font-weight: 800;");

        // Quick Jump Presets
        Button btnCampus = createNavButton("Campus Overview", () -> runScript("window.resetCampusView();"));
        Button btnSjt = createNavButton("SJT Quad", () -> runScript("window.focusArea('SJT');"));
        Button btnCdmm = createNavButton("CDMM Block", () -> runScript("window.focusArea('CDMM');"));
        Button btnWoodys = createNavButton("Woody's Arena", () -> runScript("window.focusArea('WOODYS');"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Tile Layer Switchers
        Label layerLbl = new Label("Layer:");
        layerLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");

        Button btnVoyager = createLayerButton("💎 Carto Glass", "voyager");
        Button btnOsm = createLayerButton("🗺️ Streets", "osm");
        Button btnSat = createLayerButton("🛰️ Satellite", "satellite");

        HBox layerGroup = new HBox(4, btnVoyager, btnOsm, btnSat);
        layerGroup.setAlignment(Pos.CENTER_LEFT);

        // Zoom Controls
        Button zoomInBtn = createIconButton("+", () -> runScript("if (window.map) { window.map.zoomIn(); }"));
        Button zoomOutBtn = createIconButton("-", () -> runScript("if (window.map) { window.map.zoomOut(); }"));
        Button resetBtn = createIconButton("⟲", () -> runScript("window.resetCampusView();"));

        bar.getChildren().addAll(title, btnCampus, btnSjt, btnCdmm, btnWoodys, spacer, layerLbl, layerGroup, zoomInBtn, zoomOutBtn, resetBtn);
        return bar;
    }

    private Button createLayerButton(String text, String layerKey) {
        Button btn = new Button(text);
        updateLayerBtnStyle(btn, layerKey.equalsIgnoreCase(currentTileMode));

        btn.setOnAction(e -> {
            this.currentTileMode = layerKey;
            runScript(String.format("window.setTileLayer('%s');", layerKey));
            // Refresh styles
            HBox parent = (HBox) btn.getParent();
            if (parent != null) {
                parent.getChildren().forEach(n -> {
                    if (n instanceof Button b) {
                        boolean isSelected = b.getText().toLowerCase().contains(layerKey.substring(0, 3));
                        updateLayerBtnStyle(b, isSelected);
                    }
                });
            }
        });
        return btn;
    }

    private void updateLayerBtnStyle(Button btn, boolean active) {
        if (active) {
            btn.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #10b981, #059669); -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5px 12px; -fx-background-radius: 10px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.35), 10, 0.2, 0, 2);");
        } else {
            btn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 5px 12px; -fx-background-radius: 10px; -fx-border-color: rgba(255, 255, 255, 0.12); -fx-border-radius: 10px; -fx-cursor: hand;");
        }
    }

    private Button createNavButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: rgba(52, 211, 153, 0.12); -fx-text-fill: #34d399; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5px 12px; -fx-background-radius: 10px; -fx-border-color: rgba(52, 211, 153, 0.28); -fx-border-radius: 10px; -fx-cursor: hand;");
        btn.setOnAction(e -> action.run());
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(52, 211, 153, 0.25); -fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5px 12px; -fx-background-radius: 10px; -fx-border-color: #34d399; -fx-border-radius: 10px; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: rgba(52, 211, 153, 0.12); -fx-text-fill: #34d399; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5px 12px; -fx-background-radius: 10px; -fx-border-color: rgba(52, 211, 153, 0.28); -fx-border-radius: 10px; -fx-cursor: hand;"));
        return btn;
    }

    private Button createIconButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-text-fill: #e2e8f0; -fx-font-size: 12px; -fx-font-weight: bold; -fx-pref-width: 32px; -fx-pref-height: 28px; -fx-background-radius: 8px; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 8px; -fx-cursor: hand;");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private HBox createBottomStrip() {
        HBox strip = new HBox(12);
        strip.setAlignment(Pos.CENTER_LEFT);
        strip.setPadding(new Insets(6, 6, 2, 6));

        VBox textBox = new VBox(2);
        statusLabel.setStyle("-fx-text-fill: #34d399; -fx-font-size: 11px; -fx-font-weight: bold;");
        selectionSummaryLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px; -fx-font-weight: 500;");
        textBox.getChildren().addAll(statusLabel, selectionSummaryLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("💡 Drag to pan • Scroll to zoom • Click campus to measure walking distance");
        hint.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");

        strip.getChildren().addAll(textBox, spacer, hint);
        return strip;
    }

    private void handlePinDropped(double lat, double lng, int distMeters, int walkMins, String nearestCode) {
        statusLabel.setText(String.format("📍 Custom Pin Dropped (%.4f, %.4f) • ~%dm (%d min walk) to %s PARKING",
                lat, lng, distMeters, walkMins, nearestCode));
    }

    private void runScript(String jsCode) {
        try {
            webEngine.executeScript(jsCode);
        } catch (Exception ex) {
            // Ignore if map is still initializing
        }
    }

    private String escapeJs(String text) {
        if (text == null) return "";
        return text.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ");
    }

    /**
     * Public bridge class exposed directly to JavaScript in Leaflet.
     */
    public class MapBridge {
        public void onParkingSelected(String code) {
            Platform.runLater(() -> selectArea(code));
        }

        public void onPinDropped(double lat, double lng, int distMeters, int walkMins, String nearestCode) {
            Platform.runLater(() -> handlePinDropped(lat, lng, distMeters, walkMins, nearestCode));
        }

        public void onMapReady() {
            Platform.runLater(() -> statusLabel.setText("● Live VIT Vellore Campus Map • Real Tile Stream Active"));
        }
    }
}
