package com.crisscross.util;

import com.crisscross.models.Reservation;
import io.nayuki.qrcodegen.QrCode;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Utility for generating authentic ISO/IEC 18004 QR codes.
 * Renders directly to JavaFX Image for UI displays and SVG strings for printable receipts.
 */
public class QRCodeUtil {

    /**
     * Generates a high-quality JavaFX WritableImage containing the scannable QR code.
     */
    public static Image generateImage(String content, int targetSize) {
        if (content == null || content.isEmpty()) {
            content = "CRISSCROSS-PASS";
        }

        try {
            QrCode qr = QrCode.encodeText(content, QrCode.Ecc.MEDIUM);
            int modules = qr.size;
            int border = 2; // quiet zone
            int totalModules = modules + border * 2;

            // Scale to desired pixel size
            int scale = Math.max(1, targetSize / totalModules);
            int imgSize = totalModules * scale;

            WritableImage image = new WritableImage(imgSize, imgSize);
            PixelWriter writer = image.getPixelWriter();

            Color darkColor = Color.rgb(15, 23, 42); // deep obsidian
            Color lightColor = Color.rgb(255, 255, 255); // pure white

            for (int y = 0; y < imgSize; y++) {
                for (int x = 0; x < imgSize; x++) {
                    int moduleX = (x / scale) - border;
                    int moduleY = (y / scale) - border;
                    boolean isDark = (moduleX >= 0 && moduleX < modules && moduleY >= 0 && moduleY < modules)
                            && qr.getModule(moduleX, moduleY);

                    writer.setColor(x, y, isDark ? darkColor : lightColor);
                }
            }
            return image;
        } catch (Exception e) {
            System.err.println("Failed to generate QR Code image: " + e.getMessage());
            return null;
        }
    }

    /**
     * Generates a standalone clean vector SVG string of the QR Code for embedding into HTML receipts.
     */
    public static String generateSvg(String content) {
        if (content == null || content.isEmpty()) {
            content = "CRISSCROSS-PASS";
        }
        try {
            QrCode qr = QrCode.encodeText(content, QrCode.Ecc.MEDIUM);
            int border = 2;
            int totalSize = qr.size + border * 2;
            StringBuilder sb = new StringBuilder();
            sb.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ")
              .append(totalSize).append(" ").append(totalSize)
              .append("' stroke='none' shape-rendering='crispEdges'>\n");
            sb.append("  <rect width='100%' height='100%' fill='#FFFFFF'/>\n");
            sb.append("  <path fill='#0F172A' d='");
            for (int y = 0; y < qr.size; y++) {
                for (int x = 0; x < qr.size; x++) {
                    if (qr.getModule(x, y)) {
                        sb.append("M").append(x + border).append(",").append(y + border).append("h1v1h-1z ");
                    }
                }
            }
            sb.append("'/>\n</svg>");
            return sb.toString();
        } catch (Exception e) {
            return "<svg viewBox='0 0 100 100'><text x='10' y='50'>QR Error</text></svg>";
        }
    }

    /**
     * Constructs a standardized Gate Scanner payload string containing all pass verification data.
     */
    public static String buildPayload(Reservation reservation) {
        if (reservation == null) {
            return "CRISSCROSS:PASS|STATUS=INVALID";
        }
        return "CRISSCROSS:PASS"
                + "|CODE=" + reservation.getBookingCode()
                + "|SLOT=" + reservation.getSlotNumber()
                + "|AREA=" + reservation.getAreaName()
                + "|VEHICLE=" + reservation.getVehicleType() + ":" + reservation.getVehicleNumber()
                + "|EVENT=" + reservation.getEventTitle()
                + "|VENUE=" + reservation.getEventVenue()
                + "|FEE=INR " + String.format("%.2f", reservation.getTotalAmount())
                + "|DURATION=4_HOURS"
                + "|OVERTIME_POLICY=12PCT_EXTRA"
                + "|STATUS=" + reservation.getStatus();
    }
}
