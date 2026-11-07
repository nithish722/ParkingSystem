package com.crisscross;

import com.crisscross.database.DBConnection;
import com.crisscross.ui.ScreenManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    static {
        System.setProperty("http.maxConnections", "48");
        System.setProperty("https.maxConnections", "48");
        System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 CrissCross/1.0");
    }

    @Override
    public void start(Stage primaryStage) {
        Thread dbInitThread = new Thread(() -> {
            DBConnection.getInstance();
        });
        dbInitThread.setDaemon(true);
        dbInitThread.start();
        ScreenManager.getInstance().init(primaryStage);
    }

    @Override
    public void stop() {
        DBConnection.getInstance().close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
