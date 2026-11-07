package com.crisscross.database;

import com.crisscross.models.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Resilient Persistent Local Database Store for CrissCross.
 * Used when MySQL Server is offline or during local demonstrations.
 * Automatically persists all CRUD mutations to disk (crisscross_local_db.dat)
 * so all added slots, areas, reservations, payments, events, and users are permanently saved.
 */
public class MockDatabaseStore {
    private static MockDatabaseStore instance;
    private static final String STORAGE_FILENAME = "crisscross_local_db.dat";

    private final Map<Integer, User> users = new ConcurrentHashMap<>();
    private final Map<Integer, Event> events = new ConcurrentHashMap<>();
    private final Map<Integer, ParkingArea> parkingAreas = new ConcurrentHashMap<>();
    private final Map<Integer, ParkingSlot> parkingSlots = new ConcurrentHashMap<>();
    private final Map<Integer, Reservation> reservations = new ConcurrentHashMap<>();
    private final Map<Integer, Notification> notifications = new ConcurrentHashMap<>();

    private final AtomicInteger userIdSeq = new AtomicInteger(100);
    private final AtomicInteger eventIdSeq = new AtomicInteger(100);
    private final AtomicInteger areaIdSeq = new AtomicInteger(100);
    private final AtomicInteger slotIdSeq = new AtomicInteger(500);
    private final AtomicInteger resIdSeq = new AtomicInteger(100);
    private final AtomicInteger notifIdSeq = new AtomicInteger(100);

    private MockDatabaseStore() {
        if (!loadFromDisk()) {
            seedInitialData();
            saveToDisk();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveToDisk, "CrissCross-DB-Shutdown-Save"));
    }

    public static synchronized MockDatabaseStore getInstance() {
        if (instance == null) {
            instance = new MockDatabaseStore();
        }
        return instance;
    }

    private File getStorageFile() {
        return Paths.get(System.getProperty("user.dir"), STORAGE_FILENAME).toFile();
    }

    @SuppressWarnings("unchecked")
    public synchronized boolean loadFromDisk() {
        File file = getStorageFile();
        if (!file.exists() || file.length() == 0) {
            return false;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Map<Integer, User> loadedUsers = (Map<Integer, User>) ois.readObject();
            Map<Integer, Event> loadedEvents = (Map<Integer, Event>) ois.readObject();
            Map<Integer, ParkingArea> loadedAreas = (Map<Integer, ParkingArea>) ois.readObject();
            Map<Integer, ParkingSlot> loadedSlots = (Map<Integer, ParkingSlot>) ois.readObject();
            Map<Integer, Reservation> loadedReservations = (Map<Integer, Reservation>) ois.readObject();
            Map<Integer, Notification> loadedNotifications = (Map<Integer, Notification>) ois.readObject();

            int uSeq = ois.readInt();
            int eSeq = ois.readInt();
            int aSeq = ois.readInt();
            int sSeq = ois.readInt();
            int rSeq = ois.readInt();
            int nSeq = ois.readInt();

            users.clear();
            users.putAll(loadedUsers);
            events.clear();
            events.putAll(loadedEvents);
            parkingAreas.clear();
            parkingAreas.putAll(loadedAreas);
            parkingSlots.clear();
            parkingSlots.putAll(loadedSlots);
            reservations.clear();
            reservations.putAll(loadedReservations);
            notifications.clear();
            notifications.putAll(loadedNotifications);

            userIdSeq.set(uSeq);
            eventIdSeq.set(eSeq);
            areaIdSeq.set(aSeq);
            slotIdSeq.set(sSeq);
            resIdSeq.set(rSeq);
            notifIdSeq.set(nSeq);

            System.out.println("[CrissCross Local DB] Loaded persistent database from " + file.getAbsolutePath());
            return !users.isEmpty() && !parkingAreas.isEmpty();
        } catch (Exception e) {
            System.err.println("[CrissCross Local DB] Could not load existing store, re-initializing: " + e.getMessage());
            return false;
        }
    }

    public synchronized void saveToDisk() {
        File file = getStorageFile();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(new HashMap<>(users));
            oos.writeObject(new HashMap<>(events));
            oos.writeObject(new HashMap<>(parkingAreas));
            oos.writeObject(new HashMap<>(parkingSlots));
            oos.writeObject(new HashMap<>(reservations));
            oos.writeObject(new HashMap<>(notifications));

            oos.writeInt(userIdSeq.get());
            oos.writeInt(eventIdSeq.get());
            oos.writeInt(areaIdSeq.get());
            oos.writeInt(slotIdSeq.get());
            oos.writeInt(resIdSeq.get());
            oos.writeInt(notifIdSeq.get());
            oos.flush();
        } catch (Exception e) {
            System.err.println("[CrissCross Local DB] Error saving to disk: " + e.getMessage());
        }
    }

    private void seedInitialData() {
        // Users
        User admin = new User(1, "System Administrator", "admin@crisscross.com", "admin", "admin123",
                "+91 9876543210", "TN-01-AD-9999", "ADMIN", "ACTIVE");
        User user1 = new User(2, "Alex Reynolds", "alex@campus.edu", "alex", "alex123",
                "+91 9845012345", "TN-22-CR-1024", "CUSTOMER", "ACTIVE");
        User user2 = new User(3, "Samantha Chen", "sam@campus.edu", "sam", "sam123",
                "+91 9811234567", "TN-22-BK-4096", "CUSTOMER", "ACTIVE");
        User user3 = new User(4, "Vikram Sharma", "vikram@campus.edu", "vikram", "vikram123",
                "+91 9790123456", "TN-22-CR-8080", "CUSTOMER", "ACTIVE");
        User userVit = new User(5, "Priya Sundaram", "priya@vit.ac.in", "priyavit", "Priya#Pass2026",
                "+91 9888877777", "TN-22-VT-2026", "CUSTOMER", "ACTIVE");

        users.put(admin.getUserId(), admin);
        users.put(user1.getUserId(), user1);
        users.put(user2.getUserId(), user2);
        users.put(user3.getUserId(), user3);
        users.put(userVit.getUserId(), userVit);

        // Events
        Event e1 = new Event(1, "Annual Tech Innovation Symposium 2026", "SJT Auditorium", "2026-10-25",
                "09:30 AM", "Premier inter-college showcase of AI, cloud robotics and quantum computing research papers.",
                "School of Computer Science", "TECH", 850, "UPCOMING");
        Event e2 = new Event(2, "Riviera Cultural Fest Gala Night", "CDMM Hall", "2026-10-28",
                "05:00 PM", "Grand musical evening, theatrical drama performances, and university dance team competitions.",
                "Student Cultural Council", "CULTURAL", 1200, "UPCOMING");
        Event e3 = new Event(3, "Inter-University Athletics & Football Meet", "Woody's Grounds", "2026-11-02",
                "08:00 AM", "High-energy track and field athletics, football championship finals, and cheering stadium crowd.",
                "Department of Physical Education", "SPORTS", 1500, "UPCOMING");
        Event e4 = new Event(4, "National Hackathon & Code Sprint", "SJT Lab Complex", "2026-11-08",
                "10:00 AM", "36-hour non-stop algorithmic coding marathon and open-source innovation challenge.",
                "IEEE Computer Society", "TECH", 600, "UPCOMING");

        events.put(e1.getEventId(), e1);
        events.put(e2.getEventId(), e2);
        events.put(e3.getEventId(), e3);
        events.put(e4.getEventId(), e4);

        // Parking Areas
        ParkingArea p1 = new ParkingArea(1, "SJT PARKING", "SJT", "BOTH", 30, 24,
                "Adjacent to Silver Jubilee Tower Main Entrance and North Quad", 0.0, "SJT Auditorium", 1, "OPEN");
        ParkingArea p2 = new ParkingArea(2, "CDMM PARKING", "CDMM", "BOTH", 30, 26,
                "Basement and Ground Lot behind CDMM Architecture Block", 0.0, "CDMM Hall", 2, "OPEN");
        ParkingArea p3 = new ParkingArea(3, "WOODY'S PARKING", "WOODYS", "BOTH", 30, 27,
                "Spacious perimeter paved lot near Woody's Food Court & Sports Arena", 0.0, "Woody's Grounds", 1, "OPEN");

        parkingAreas.put(p1.getAreaId(), p1);
        parkingAreas.put(p2.getAreaId(), p2);
        parkingAreas.put(p3.getAreaId(), p3);

        // Generate Slots for SJT (1..30)
        int slotId = 1;
        for (int i = 1; i <= 15; i++) {
            String slotNum = String.format("SJT-C%02d", i);
            String status = (i == 3) ? "OCCUPIED" : (i == 6) ? "RESERVED" : (i == 11) ? "BLOCKED" : "AVAILABLE";
            ParkingSlot slot = new ParkingSlot(slotId++, 1, slotNum, "CAR", "Ground Level", status);
            slot.setAreaName("SJT PARKING");
            parkingSlots.put(slot.getSlotId(), slot);
        }
        for (int i = 1; i <= 15; i++) {
            String slotNum = String.format("SJT-B%02d", i);
            String status = (i == 4) ? "OCCUPIED" : "AVAILABLE";
            ParkingSlot slot = new ParkingSlot(slotId++, 1, slotNum, "BIKE", "North Bay", status);
            slot.setAreaName("SJT PARKING");
            parkingSlots.put(slot.getSlotId(), slot);
        }

        // CDMM (31..60)
        for (int i = 1; i <= 15; i++) {
            String slotNum = String.format("CDMM-C%02d", i);
            String status = (i == 5) ? "RESERVED" : "AVAILABLE";
            ParkingSlot slot = new ParkingSlot(slotId++, 2, slotNum, "CAR", "Lower Deck", status);
            slot.setAreaName("CDMM PARKING");
            parkingSlots.put(slot.getSlotId(), slot);
        }
        for (int i = 1; i <= 15; i++) {
            String slotNum = String.format("CDMM-B%02d", i);
            String status = (i == 3) ? "OCCUPIED" : "AVAILABLE";
            ParkingSlot slot = new ParkingSlot(slotId++, 2, slotNum, "BIKE", "South Ramp", status);
            slot.setAreaName("CDMM PARKING");
            parkingSlots.put(slot.getSlotId(), slot);
        }

        // Woody's (61..90)
        for (int i = 1; i <= 15; i++) {
            String slotNum = String.format("WDY-C%02d", i);
            String status = "AVAILABLE";
            ParkingSlot slot = new ParkingSlot(slotId++, 3, slotNum, "CAR", "Main Plaza", status);
            slot.setAreaName("WOODY'S PARKING");
            parkingSlots.put(slot.getSlotId(), slot);
        }
        for (int i = 1; i <= 15; i++) {
            String slotNum = String.format("WDY-B%02d", i);
            String status = (i == 5) ? "OCCUPIED" : "AVAILABLE";
            ParkingSlot slot = new ParkingSlot(slotId++, 3, slotNum, "BIKE", "East Canopy", status);
            slot.setAreaName("WOODY'S PARKING");
            parkingSlots.put(slot.getSlotId(), slot);
        }

        // Reservations
        Reservation r1 = new Reservation(1, "CRISS-2026-X892", 2, 1, 1, 6, "CAR", "TN-22-CR-1024",
                LocalDateTime.now().minusHours(2), 4, 0.0, "CONFIRMED", "Tech Symposium VIP Speaker Area pass");
        r1.setUserName("Alex Reynolds");
        r1.setEventTitle("Annual Tech Innovation Symposium 2026");
        r1.setEventVenue("SJT Auditorium");
        r1.setAreaName("SJT PARKING");
        r1.setSlotNumber("SJT-C06");

        Reservation r2 = new Reservation(2, "CRISS-2026-B104", 3, 2, 2, 35, "CAR", "TN-22-BK-4096",
                LocalDateTime.now().minusDays(1), 5, 0.0, "CONFIRMED", "Riviera Stage Crew parking reservation");
        r2.setUserName("Samantha Chen");
        r2.setEventTitle("Riviera Cultural Fest Gala Night");
        r2.setEventVenue("CDMM Hall");
        r2.setAreaName("CDMM PARKING");
        r2.setSlotNumber("CDMM-C05");

        Reservation r3 = new Reservation(3, "CRISS-2026-C771", 4, 3, 3, 65, "BIKE", "TN-22-CR-8080",
                LocalDateTime.now().minusDays(3), 2, 0.0, "COMPLETED", "Athletics meet participant entry");
        r3.setUserName("Vikram Sharma");
        r3.setEventTitle("Inter-University Athletics & Football Meet");
        r3.setEventVenue("Woody's Grounds");
        r3.setAreaName("WOODY'S PARKING");
        r3.setSlotNumber("WDY-B05");

        reservations.put(r1.getReservationId(), r1);
        reservations.put(r2.getReservationId(), r2);
        reservations.put(r3.getReservationId(), r3);

        // Notifications
        Notification n1 = new Notification(1, "Welcome to CrissCross 2026",
                "Welcome to CrissCross smart event parking management system. Enjoy seamless glassmorphic campus parking!",
                "INFO", "ALL", LocalDateTime.now());
        Notification n2 = new Notification(2, "Heavy Inflow Expected near SJT",
                "Tech Innovation Symposium has over 800 registered visitors. Please book your slots in SJT or backup CDMM early.",
                "WARNING", "CUSTOMER", LocalDateTime.now());
        Notification n3 = new Notification(3, "Woody's Parking Gate 2 Operational",
                "Woody's parking south gate has been reopened for bike direct access.",
                "INFO", "ALL", LocalDateTime.now());
        Notification n4 = new Notification(4, "Maintenance Notice: Slot SJT-C11",
                "Slot SJT-C11 is undergoing sensor calibration and is currently marked blocked.",
                "URGENT", "ADMIN", LocalDateTime.now());

        notifications.put(n1.getNotificationId(), n1);
        notifications.put(n2.getNotificationId(), n2);
        notifications.put(n3.getNotificationId(), n3);
        notifications.put(n4.getNotificationId(), n4);
    }

    // Accessors
    public Map<Integer, User> getUsers() { return users; }
    public Map<Integer, Event> getEvents() { return events; }
    public Map<Integer, ParkingArea> getParkingAreas() { return parkingAreas; }
    public Map<Integer, ParkingSlot> getParkingSlots() { return parkingSlots; }
    public Map<Integer, Reservation> getReservations() { return reservations; }
    public Map<Integer, Notification> getNotifications() { return notifications; }

    public int nextUserId() { return userIdSeq.incrementAndGet(); }
    public int nextEventId() { return eventIdSeq.incrementAndGet(); }
    public int nextAreaId() { return areaIdSeq.incrementAndGet(); }
    public int nextSlotId() { return slotIdSeq.incrementAndGet(); }
    public int nextReservationId() { return resIdSeq.incrementAndGet(); }
    public int nextNotificationId() { return notifIdSeq.incrementAndGet(); }
}
