package com.crisscross;

/**
 * Secondary launcher class to bypass JavaFX module path restrictions
 * when executing fat JARs or standard Java main commands.
 */
public class Launcher {
    public static void main(String[] args) {
        // Boost HTTP concurrency for map tile streaming
        System.setProperty("http.maxConnections", "48");
        System.setProperty("https.maxConnections", "48");
        System.setProperty("http.agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 CrissCross/1.0");

        MainApp.main(args);
    }
}
