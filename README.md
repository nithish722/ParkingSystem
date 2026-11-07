# CrissCross — Smart Event Parking Management System

> **A Pure Java Desktop Application for Windows** built with **JavaFX**, **Apple-Inspired Glassmorphic CSS**, **MySQL JDBC Connectivity**, and **Multi-Layered Architecture (Models, Services, DAO, Controllers, UI)**.

---

## 1. Project Overview & Technology Stack

| Specification | Technology / Tool | Details |
| :--- | :--- | :--- |
| **Application Type** | Java Desktop GUI Application | Designed specifically for Windows desktop |
| **Programming Language** | Java (JDK 21 / 25 LTS) | Pure Java implementation (No HTML, No JS) |
| **Frontend Framework** | JavaFX (21.0.2) | `Stage`, `Scene`, `BorderPane`, `VBox`, `HBox`, `TableView`, `Button` |
| **UI Styling** | JavaFX CSS (`-fx-...`) | Apple-inspired Glassmorphism (`glass-dark.css` & `glass-light.css`) |
| **UI Aesthetics** | Apple Frosted Glass | Translucent panels, blur/drop-shadows, 18–26px rounded corners, cyan & blue accents |
| **Database** | MySQL 8.x | Relational database with full normalization and foreign keys |
| **Database Connectivity** | JDBC (`com.mysql.cj.jdbc.Driver`) | `mysql-connector-j` 8.3.0 |
| **Database Tool** | MySQL Workbench | `database_schema.sql` provided in root & resources |
| **Build Tool** | Apache Maven | Complete `pom.xml` with `javafx-maven-plugin` & `mvnw.cmd` |
| **Main IDE** | Visual Studio Code | Configured `.vscode/settings.json` and `launch.json` |
| **Resilience & Fallback** | Hybrid Mode | Connects to live MySQL, or seamless local persistence if MySQL is offline |

---

## 2. CrissCross Parking Rules & Proximity Recommendation Engine

CrissCross implements smart proximity matching between **campus event venues** and the **three designated parking zones**:

### Designated Parking Zones:
1. **SJT PARKING** (`SJT`):
   - **Proximity:** Adjacent to Silver Jubilee Tower, North Quad & CS Labs (~50m, 1-min walk).
   - **Primary Allocation:** Recommended for Tech Symposiums, Hackathons & Academic Summits held at **SJT Auditorium** or **SJT Lab Complex**.
   - **Designated Backup Area:** **CDMM PARKING**.
2. **CDMM PARKING** (`CDMM`):
   - **Proximity:** Behind Architecture Block & Central Quad (~65m, 1-min walk).
   - **Primary Allocation:** Recommended for Cultural Galas, Drama Nights & Design Exhibitions held at **CDMM Hall**.
   - **Designated Backup Area:** **WOODY'S PARKING**.
3. **WOODY'S PARKING** (`WOODYS`):
   - **Proximity:** Perimeter lot near Outdoor Amphitheater & Food Court (~40m, 1-min walk).
   - **Primary Allocation:** Recommended for Sports Tournaments, Cheering Stadiums & Open-Air Fests held at **Woody's Grounds**.
   - **Designated Backup Area:** **SJT PARKING**.

### Vehicle Classification & Bay Types:
- **Four-Wheelers (Car):** Designated automotive bays (`SJT-C01` to `SJT-C15`, `CDMM-C01` to `CDMM-C15`, `WDY-C01` to `WDY-C15`).
- **Two-Wheelers (Bike):** Shaded canopy motorcycle & scooter bays (`SJT-B01` to `SJT-B15`, `CDMM-B01` to `CDMM-B15`, `WDY-B01` to `WDY-B15`).
- **Capacity Monitoring:** If the primary parking zone is congested (>90% full or fewer than 2 slots remaining for the vehicle type), the Smart Engine automatically raises a traffic advisory and promotes the **Backup Parking Zone**.

---

## 3. Screen Inventory & Workflow

### Customer Experience (22 Screens):
1. **Splash Screen:** App logo, glowing pulse effect, name, and animated progress loading bar.
2. **Welcome Screen:** High-level overview, 3 parking zone feature cards, and quick demo buttons.
3. **Login Screen:** Username/Email, password, role check, demo auto-fill pills, and theme toggle.
4. **Registration Screen:** Full Name, Email, Username, Phone, Default Vehicle Plate, Password, and Confirmation.
5. **Forgot Password Screen:** Verification by email/username and password recovery workflow.
6. **Customer Dashboard:** Personalized greeting, KPI cards (Active Passes, Events, Available Slots), and live parking gauges.
7. **Event Catalog:** Searchable and category-filterable list of campus symposiums, cultural fests, and sports meets.
8. **Event Details:** In-depth event schedule, venue, expected footfall, organizer, and proximity match preview.
9. **Vehicle Selection:** Interactive toggle between Four-Wheeler (Car) and Two-Wheeler (Bike) with plate number input.
10. **Parking Availability:** Capacity comparison across SJT, CDMM, and Woody's with recommended badges.
11. **Smart Recommendation:** Real-time proximity engine calculating match %, walking minutes, and primary/backup options.
12. **Parking Area Details:** Detailed metrics, floor levels, security status, and free slot counts for chosen zone.
13. **Interactive Parking Map:** Campus schematic with SJT, CDMM, Woody's, walkways, distances, and clickable lot pins.
14. **Slot Selection Grid:** Color-coded slot bays (`AVAILABLE` 🟢, `RESERVED` 🟡, `OCCUPIED` 🔴, `BLOCKED` ⚪).
15. **Booking Summary:** Detailed review of event, attendee, vehicle, parking zone, slot, and complimentary event pass status.
16. **Booking Confirmation:** Unique Booking ID (`CRISS-2026-XXXX`), digital QR pass badge, and download receipt.
17. **My Reservations:** History table of confirmed, completed, and cancelled bookings with quick cancel action.
18. **Reservation Details:** Modal inspect view of a single pass with barcode and directions.
19. **Profile View:** View and update name, email, phone, default plate number, and password.
20. **Notifications & Announcements:** Real-time campus traffic advisories, gate updates, and priority alerts.
21. **Help & About:** Campus parking rules, architecture specifications, team credits, and FAQ.
22. **Logout Confirmation:** Modal dialog to safely end session and return to login.

### Administrator Console (18 Requirements from Image 1):
1. **Admin Login:** Authorized credentials validation with role access enforcement.
2. **Admin Dashboard:** High-level KPIs (Total Bookings, Active Events, Occupancy %, User Count) and live tables.
3. **Event Management:** Table of all events with instant search, category filters, and management tools.
4. **Add Event:** Modal form configuring title, venue (SJT, CDMM, Woody's), schedule, and organizer.
5. **Edit Event:** Modal form updating event information and status in database.
6. **Parking-Area Management:** List of campus zones with vehicle types, rates, and servicing venues.
7. **Add Parking Area:** Configure new parking zones, code, vehicle compatibility, and total capacity.
8. **Edit Parking Area:** Update parking configuration and maintenance flags.
9. **Slot Management:** Comprehensive table of all slots filterable by Area, Vehicle Type, and Status.
10. **Add/Edit Slots:** Modal to create slots (e.g. `SJT-C16`), set floor levels, and assign bay types.
11. **Parking Status Management:** Real-time interactive grid to immediately toggle slots: Available, Occupied, Blocked.
12. **Reservation Management:** Full audit table of all customer bookings across campus.
13. **Reservation Details:** Detailed modal inspecting customer info, timestamp, vehicle, and gate check-in status.
14. **User Management:** Registered user accounts table with status toggling (Active vs Suspended).
15. **Notification Management:** Publish campus announcements with priority levels (Info, Warning, Urgent).
16. **Reports Dashboard:** Analytical charts for zone occupancy ratios, Car vs Bike distributions, and volume trends.
17. **Admin Profile:** Administrator credentials, contact info, and MySQL connection test/reconnect diagnostics.
18. **Logout:** Safe administrator session termination with confirmation modal.

---

## 4. Database Schema (`crisscross_db`)

The database is defined in `database_schema.sql` (and `src/main/resources/sql/schema.sql`):
- `users`: `user_id`, `full_name`, `email`, `username`, `password`, `phone`, `vehicle_number`, `role`, `status`, `created_at`
- `events`: `event_id`, `title`, `venue`, `event_date`, `event_time`, `description`, `organizer`, `category`, `expected_attendees`, `status`, `created_at`
- `parking_areas`: `area_id`, `name`, `code`, `vehicle_type`, `total_capacity`, `available_capacity`, `location_desc`, `hourly_rate`, `nearest_venue`, `walking_time_mins`, `status`
- `parking_slots`: `slot_id`, `area_id`, `slot_number`, `slot_type`, `floor_level`, `status` (Foreign key to `parking_areas`)
- `reservations`: `reservation_id`, `booking_code`, `user_id`, `event_id`, `area_id`, `slot_id`, `vehicle_type`, `vehicle_number`, `booking_time`, `duration_hours`, `total_amount`, `status`, `notes` (Foreign keys to `users`, `events`, `parking_areas`, `parking_slots`)
- `notifications`: `notification_id`, `title`, `message`, `priority`, `target_role`, `created_at`

### Setting up in MySQL Workbench:
1. Open **MySQL Workbench**.
2. Connect to your local MySQL Server (`localhost:3306`).
3. Click **File → Open SQL Script...** and select `database_schema.sql` from this folder.
4. Click the ⚡ **Execute** button.
5. In `src/main/resources/config.properties`, confirm your MySQL password if you have set one for `root` (defaults to blank).

---

## 5. How to Run the Application

### Option A — Using the Windows Launcher (Easiest):
Simply double-click:
```cmd
run.bat
```

### Option B — Using the Maven Wrapper in Terminal:
Open PowerShell or Command Prompt in the project folder and run:
```powershell
.\mvnw.cmd javafx:run
```

### Option C — Running inside VS Code:
1. Open this folder in **VS Code**.
2. Open [MainApp.java](file:///c:/Users/User/Documents/java%20course%20project/java%20management%20final%20project/src/main/java/com/crisscross/MainApp.java).
3. Press `F5` or click **Run → Start Debugging** (configured via `.vscode/launch.json`).

---

## 6. Pre-Configured Test Credentials

| Role | Username / Email | Password | Access Level |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` (or `admin@crisscross.com`) | `admin123` | Full 18-Screen Admin Console |
| **Customer** | `alex` (or `alex@campus.edu`) | `alex123` | Full 22-Screen Customer Portal |
| **Customer (Bike)** | `sam` (or `sam@campus.edu`) | `sam123` | Pre-registered Two-Wheeler User |

*(Both login screens include quick **"Fill Demo"** buttons for instant one-click login during examinations and presentations).*
