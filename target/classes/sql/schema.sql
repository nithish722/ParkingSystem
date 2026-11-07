-- ========================================================
-- CrissCross Smart Event Parking Management System
-- Database Schema & Comprehensive Initial Seed Data
-- Target Database: MySQL 8.x / MySQL Workbench
-- ========================================================

CREATE DATABASE IF NOT EXISTS crisscross_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE crisscross_db;

-- --------------------------------------------------------
-- 1. Table: users
-- --------------------------------------------------------
DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS parking_slots;
DROP TABLE IF EXISTS parking_areas;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    phone VARCHAR(25),
    vehicle_number VARCHAR(25),
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER', -- 'CUSTOMER', 'ADMIN'
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- 'ACTIVE', 'SUSPENDED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- --------------------------------------------------------
-- 2. Table: events
-- --------------------------------------------------------
CREATE TABLE events (
    event_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    venue VARCHAR(100) NOT NULL, -- e.g., 'SJT Auditorium', 'CDMM Hall', 'Woody\'s Grounds'
    event_date VARCHAR(50) NOT NULL, -- Format: YYYY-MM-DD or readable string
    event_time VARCHAR(50) NOT NULL, -- Format: HH:MM AM/PM
    description TEXT,
    organizer VARCHAR(100),
    category VARCHAR(50) DEFAULT 'ACADEMIC', -- 'TECH', 'CULTURAL', 'SPORTS', 'SYMPOSIUM'
    expected_attendees INT DEFAULT 300,
    status VARCHAR(20) DEFAULT 'UPCOMING', -- 'UPCOMING', 'ONGOING', 'COMPLETED', 'CANCELLED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- --------------------------------------------------------
-- 3. Table: parking_areas
-- --------------------------------------------------------
CREATE TABLE parking_areas (
    area_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL, -- 'SJT PARKING', 'CDMM PARKING', 'WOODY\'S PARKING'
    code VARCHAR(30) NOT NULL UNIQUE,
    vehicle_type VARCHAR(20) NOT NULL DEFAULT 'BOTH', -- 'CAR', 'BIKE', 'BOTH'
    total_capacity INT NOT NULL DEFAULT 40,
    available_capacity INT NOT NULL DEFAULT 40,
    location_desc VARCHAR(255),
    hourly_rate DECIMAL(10,2) DEFAULT 0.00,
    nearest_venue VARCHAR(100) NOT NULL,
    walking_time_mins INT DEFAULT 2,
    status VARCHAR(20) DEFAULT 'OPEN' -- 'OPEN', 'FULL', 'MAINTENANCE'
) ENGINE=InnoDB;

-- --------------------------------------------------------
-- 4. Table: parking_slots
-- --------------------------------------------------------
CREATE TABLE parking_slots (
    slot_id INT AUTO_INCREMENT PRIMARY KEY,
    area_id INT NOT NULL,
    slot_number VARCHAR(30) NOT NULL,
    slot_type VARCHAR(20) NOT NULL DEFAULT 'CAR', -- 'CAR', 'BIKE'
    floor_level VARCHAR(30) DEFAULT 'Ground Level',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', -- 'AVAILABLE', 'RESERVED', 'OCCUPIED', 'BLOCKED'
    FOREIGN KEY (area_id) REFERENCES parking_areas(area_id) ON DELETE CASCADE,
    UNIQUE KEY uk_area_slot (area_id, slot_number)
) ENGINE=InnoDB;

-- --------------------------------------------------------
-- 5. Table: reservations
-- --------------------------------------------------------
CREATE TABLE reservations (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_code VARCHAR(50) NOT NULL UNIQUE,
    user_id INT NOT NULL,
    event_id INT NOT NULL,
    area_id INT NOT NULL,
    slot_id INT NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL DEFAULT 'CAR', -- 'CAR', 'BIKE'
    vehicle_number VARCHAR(30) NOT NULL,
    booking_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    duration_hours INT DEFAULT 3,
    total_amount DECIMAL(10,2) DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED', -- 'CONFIRMED', 'ACTIVE', 'COMPLETED', 'CANCELLED'
    notes VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE,
    FOREIGN KEY (area_id) REFERENCES parking_areas(area_id) ON DELETE CASCADE,
    FOREIGN KEY (slot_id) REFERENCES parking_slots(slot_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- --------------------------------------------------------
-- 6. Table: notifications
-- --------------------------------------------------------
CREATE TABLE notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    priority VARCHAR(20) DEFAULT 'INFO', -- 'INFO', 'WARNING', 'URGENT'
    target_role VARCHAR(20) DEFAULT 'ALL', -- 'ALL', 'CUSTOMER', 'ADMIN'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ========================================================
-- SEED DATA
-- ========================================================

-- Users (Password stored in plain/hash for course demo; admin & test students)
INSERT INTO users (full_name, email, username, password, phone, vehicle_number, role, status) VALUES
('System Administrator', 'admin@crisscross.com', 'admin', 'admin123', '+91 9876543210', 'TN-01-AD-9999', 'ADMIN', 'ACTIVE'),
('Alex Reynolds', 'alex@campus.edu', 'alex', 'alex123', '+91 9845012345', 'TN-22-CR-1024', 'CUSTOMER', 'ACTIVE'),
('Samantha Chen', 'sam@campus.edu', 'sam', 'sam123', '+91 9811234567', 'TN-22-BK-4096', 'CUSTOMER', 'ACTIVE'),
('Vikram Sharma', 'vikram@campus.edu', 'vikram', 'vikram123', '+91 9790123456', 'TN-22-CR-8080', 'CUSTOMER', 'ACTIVE'),
('Priya Sundaram', 'priya@vit.ac.in', 'priyavit', 'Priya#Pass2026', '+91 9888877777', 'TN-22-VT-2026', 'CUSTOMER', 'ACTIVE');

-- Events
INSERT INTO events (title, venue, event_date, event_time, description, organizer, category, expected_attendees, status) VALUES
('Annual Tech Innovation Symposium 2026', 'SJT Auditorium', '2026-10-25', '09:30 AM', 'Premier inter-college showcase of AI, cloud robotics and quantum computing research papers.', 'School of Computer Science', 'TECH', 850, 'UPCOMING'),
('Riviera Cultural Fest Gala Night', 'CDMM Hall', '2026-10-28', '05:00 PM', 'Grand musical evening, theatrical drama performances, and university dance team competitions.', 'Student Cultural Council', 'CULTURAL', 1200, 'UPCOMING'),
('Inter-University Athletics & Football Meet', 'Woody\'s Grounds', '2026-11-02', '08:00 AM', 'High-energy track and field athletics, football championship finals, and cheering stadium crowd.', 'Department of Physical Education', 'SPORTS', 1500, 'UPCOMING'),
('National Hackathon & Code Sprint', 'SJT Lab Complex', '2026-11-08', '10:00 AM', '36-hour non-stop algorithmic coding marathon and open-source innovation challenge.', 'IEEE Computer Society', 'TECH', 600, 'UPCOMING');

-- Parking Areas (SJT, CDMM, Woody\'s)
INSERT INTO parking_areas (name, code, vehicle_type, total_capacity, available_capacity, location_desc, hourly_rate, nearest_venue, walking_time_mins, status) VALUES
('SJT PARKING', 'SJT', 'BOTH', 30, 24, 'Adjacent to Silver Jubilee Tower Main Entrance and North Quad', 0.00, 'SJT Auditorium', 1, 'OPEN'),
('CDMM PARKING', 'CDMM', 'BOTH', 30, 26, 'Basement and Ground Lot behind CDMM Architecture Block', 0.00, 'CDMM Hall', 2, 'OPEN'),
('WOODY\'S PARKING', 'WOODYS', 'BOTH', 30, 27, 'Spacious perimeter paved lot near Woody\'s Food Court & Sports Arena', 0.00, 'Woody\'s Grounds', 1, 'OPEN');

-- Parking Slots for SJT (15 Cars, 15 Bikes)
INSERT INTO parking_slots (area_id, slot_number, slot_type, floor_level, status) VALUES
(1, 'SJT-C01', 'CAR', 'Ground Level', 'AVAILABLE'),
(1, 'SJT-C02', 'CAR', 'Ground Level', 'AVAILABLE'),
(1, 'SJT-C03', 'CAR', 'Ground Level', 'OCCUPIED'),
(1, 'SJT-C04', 'CAR', 'Ground Level', 'AVAILABLE'),
(1, 'SJT-C05', 'CAR', 'Ground Level', 'AVAILABLE'),
(1, 'SJT-C06', 'CAR', 'Ground Level', 'RESERVED'),
(1, 'SJT-C07', 'CAR', 'Ground Level', 'AVAILABLE'),
(1, 'SJT-C08', 'CAR', 'Ground Level', 'AVAILABLE'),
(1, 'SJT-C09', 'CAR', 'Ground Level', 'AVAILABLE'),
(1, 'SJT-C10', 'CAR', 'Ground Level', 'AVAILABLE'),
(1, 'SJT-C11', 'CAR', 'Ground Level', 'BLOCKED'),
(1, 'SJT-C12', 'CAR', 'Ground Level', 'AVAILABLE'),
(1, 'SJT-C13', 'CAR', 'Ground Level', 'AVAILABLE'),
(1, 'SJT-C14', 'CAR', 'Ground Level', 'AVAILABLE'),
(1, 'SJT-C15', 'CAR', 'Ground Level', 'AVAILABLE'),
(1, 'SJT-B01', 'BIKE', 'North Bay', 'AVAILABLE'),
(1, 'SJT-B02', 'BIKE', 'North Bay', 'AVAILABLE'),
(1, 'SJT-B03', 'BIKE', 'North Bay', 'AVAILABLE'),
(1, 'SJT-B04', 'BIKE', 'North Bay', 'OCCUPIED'),
(1, 'SJT-B05', 'BIKE', 'North Bay', 'AVAILABLE'),
(1, 'SJT-B06', 'BIKE', 'North Bay', 'AVAILABLE'),
(1, 'SJT-B07', 'BIKE', 'North Bay', 'AVAILABLE'),
(1, 'SJT-B08', 'BIKE', 'North Bay', 'AVAILABLE'),
(1, 'SJT-B09', 'BIKE', 'North Bay', 'AVAILABLE'),
(1, 'SJT-B10', 'BIKE', 'North Bay', 'AVAILABLE'),
(1, 'SJT-B11', 'BIKE', 'North Bay', 'AVAILABLE'),
(1, 'SJT-B12', 'BIKE', 'North Bay', 'AVAILABLE'),
(1, 'SJT-B13', 'BIKE', 'North Bay', 'AVAILABLE'),
(1, 'SJT-B14', 'BIKE', 'North Bay', 'AVAILABLE'),
(1, 'SJT-B15', 'BIKE', 'North Bay', 'AVAILABLE');

-- Parking Slots for CDMM (15 Cars, 15 Bikes)
INSERT INTO parking_slots (area_id, slot_number, slot_type, floor_level, status) VALUES
(2, 'CDMM-C01', 'CAR', 'Lower Deck', 'AVAILABLE'),
(2, 'CDMM-C02', 'CAR', 'Lower Deck', 'AVAILABLE'),
(2, 'CDMM-C03', 'CAR', 'Lower Deck', 'AVAILABLE'),
(2, 'CDMM-C04', 'CAR', 'Lower Deck', 'AVAILABLE'),
(2, 'CDMM-C05', 'CAR', 'Lower Deck', 'RESERVED'),
(2, 'CDMM-C06', 'CAR', 'Lower Deck', 'AVAILABLE'),
(2, 'CDMM-C07', 'CAR', 'Lower Deck', 'AVAILABLE'),
(2, 'CDMM-C08', 'CAR', 'Lower Deck', 'AVAILABLE'),
(2, 'CDMM-C09', 'CAR', 'Lower Deck', 'AVAILABLE'),
(2, 'CDMM-C10', 'CAR', 'Lower Deck', 'AVAILABLE'),
(2, 'CDMM-C11', 'CAR', 'Lower Deck', 'AVAILABLE'),
(2, 'CDMM-C12', 'CAR', 'Lower Deck', 'AVAILABLE'),
(2, 'CDMM-C13', 'CAR', 'Lower Deck', 'AVAILABLE'),
(2, 'CDMM-C14', 'CAR', 'Lower Deck', 'AVAILABLE'),
(2, 'CDMM-C15', 'CAR', 'Lower Deck', 'AVAILABLE'),
(2, 'CDMM-B01', 'BIKE', 'South Ramp', 'AVAILABLE'),
(2, 'CDMM-B02', 'BIKE', 'South Ramp', 'AVAILABLE'),
(2, 'CDMM-B03', 'BIKE', 'South Ramp', 'OCCUPIED'),
(2, 'CDMM-B04', 'BIKE', 'South Ramp', 'AVAILABLE'),
(2, 'CDMM-B05', 'BIKE', 'South Ramp', 'AVAILABLE'),
(2, 'CDMM-B06', 'BIKE', 'South Ramp', 'AVAILABLE'),
(2, 'CDMM-B07', 'BIKE', 'South Ramp', 'AVAILABLE'),
(2, 'CDMM-B08', 'BIKE', 'South Ramp', 'AVAILABLE'),
(2, 'CDMM-B09', 'BIKE', 'South Ramp', 'AVAILABLE'),
(2, 'CDMM-B10', 'BIKE', 'South Ramp', 'AVAILABLE'),
(2, 'CDMM-B11', 'BIKE', 'South Ramp', 'AVAILABLE'),
(2, 'CDMM-B12', 'BIKE', 'South Ramp', 'AVAILABLE'),
(2, 'CDMM-B13', 'BIKE', 'South Ramp', 'AVAILABLE'),
(2, 'CDMM-B14', 'BIKE', 'South Ramp', 'AVAILABLE'),
(2, 'CDMM-B15', 'BIKE', 'South Ramp', 'AVAILABLE');

-- Parking Slots for Woody\'s (15 Cars, 15 Bikes)
INSERT INTO parking_slots (area_id, slot_number, slot_type, floor_level, status) VALUES
(3, 'WDY-C01', 'CAR', 'Main Plaza', 'AVAILABLE'),
(3, 'WDY-C02', 'CAR', 'Main Plaza', 'AVAILABLE'),
(3, 'WDY-C03', 'CAR', 'Main Plaza', 'AVAILABLE'),
(3, 'WDY-C04', 'CAR', 'Main Plaza', 'AVAILABLE'),
(3, 'WDY-C05', 'CAR', 'Main Plaza', 'AVAILABLE'),
(3, 'WDY-C06', 'CAR', 'Main Plaza', 'AVAILABLE'),
(3, 'WDY-C07', 'CAR', 'Main Plaza', 'AVAILABLE'),
(3, 'WDY-C08', 'CAR', 'Main Plaza', 'AVAILABLE'),
(3, 'WDY-C09', 'CAR', 'Main Plaza', 'AVAILABLE'),
(3, 'WDY-C10', 'CAR', 'Main Plaza', 'AVAILABLE'),
(3, 'WDY-C11', 'CAR', 'Main Plaza', 'AVAILABLE'),
(3, 'WDY-C12', 'CAR', 'Main Plaza', 'AVAILABLE'),
(3, 'WDY-C13', 'CAR', 'Main Plaza', 'AVAILABLE'),
(3, 'WDY-C14', 'CAR', 'Main Plaza', 'AVAILABLE'),
(3, 'WDY-C15', 'CAR', 'Main Plaza', 'AVAILABLE'),
(3, 'WDY-B01', 'BIKE', 'East Canopy', 'AVAILABLE'),
(3, 'WDY-B02', 'BIKE', 'East Canopy', 'AVAILABLE'),
(3, 'WDY-B03', 'BIKE', 'East Canopy', 'AVAILABLE'),
(3, 'WDY-B04', 'BIKE', 'East Canopy', 'AVAILABLE'),
(3, 'WDY-B05', 'BIKE', 'East Canopy', 'OCCUPIED'),
(3, 'WDY-B06', 'BIKE', 'East Canopy', 'AVAILABLE'),
(3, 'WDY-B07', 'BIKE', 'East Canopy', 'AVAILABLE'),
(3, 'WDY-B08', 'BIKE', 'East Canopy', 'AVAILABLE'),
(3, 'WDY-B09', 'BIKE', 'East Canopy', 'AVAILABLE'),
(3, 'WDY-B10', 'BIKE', 'East Canopy', 'AVAILABLE'),
(3, 'WDY-B11', 'BIKE', 'East Canopy', 'AVAILABLE'),
(3, 'WDY-B12', 'BIKE', 'East Canopy', 'AVAILABLE'),
(3, 'WDY-B13', 'BIKE', 'East Canopy', 'AVAILABLE'),
(3, 'WDY-B14', 'BIKE', 'East Canopy', 'AVAILABLE'),
(3, 'WDY-B15', 'BIKE', 'East Canopy', 'AVAILABLE');

-- Reservations
INSERT INTO reservations (booking_code, user_id, event_id, area_id, slot_id, vehicle_type, vehicle_number, booking_time, duration_hours, total_amount, status, notes) VALUES
('CRISS-2026-X892', 2, 1, 1, 6, 'CAR', 'TN-22-CR-1024', NOW() - INTERVAL 2 HOUR, 4, 0.00, 'CONFIRMED', 'Tech Symposium VIP Speaker Area entry pass'),
('CRISS-2026-B104', 3, 2, 2, 35, 'CAR', 'TN-22-BK-4096', NOW() - INTERVAL 1 DAY, 5, 0.00, 'CONFIRMED', 'Riviera Stage Crew parking reservation'),
('CRISS-2026-C771', 4, 3, 3, 65, 'BIKE', 'TN-22-CR-8080', NOW() - INTERVAL 3 DAY, 2, 0.00, 'COMPLETED', 'Athletics meet participant entry');

-- Notifications
INSERT INTO notifications (title, message, priority, target_role, created_at) VALUES
('Welcome to CrissCross 2026', 'Welcome to CrissCross smart event parking management system. Enjoy seamless glassmorphic campus parking!', 'INFO', 'ALL', NOW()),
('Heavy Inflow Expected near SJT', 'Tech Innovation Symposium has over 800 registered visitors. Please book your slots in SJT or backup CDMM early.', 'WARNING', 'CUSTOMER', NOW()),
('Woody\'s Parking Gate 2 Operational', 'Woody\'s parking south gate has been reopened for bike direct access.', 'INFO', 'ALL', NOW()),
('Maintenance Notice: Slot SJT-C11', 'Slot SJT-C11 is undergoing sensor calibration and is currently marked blocked.', 'URGENT', 'ADMIN', NOW());
