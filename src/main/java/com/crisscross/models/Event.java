package com.crisscross.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Event implements Serializable {
    private static final long serialVersionUID = 1L;
    private int eventId;
    private String title;
    private String venue; // e.g., 'SJT Auditorium', 'CDMM Hall', 'Woody\'s Grounds'
    private String eventDate;
    private String eventTime;
    private String description;
    private String organizer;
    private String category;
    private int expectedAttendees;
    private String status; // 'UPCOMING', 'ONGOING', 'COMPLETED', 'CANCELLED'
    private LocalDateTime createdAt;

    public Event() {
        this.status = "UPCOMING";
        this.category = "TECH";
        this.expectedAttendees = 300;
        this.createdAt = LocalDateTime.now();
    }

    public Event(int eventId, String title, String venue, String eventDate, String eventTime,
                 String description, String organizer, String category, int expectedAttendees, String status) {
        this.eventId = eventId;
        this.title = title;
        this.venue = venue;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.description = description;
        this.organizer = organizer;
        this.category = category;
        this.expectedAttendees = expectedAttendees;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getEventDate() { return eventDate; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }

    public String getEventTime() { return eventTime; }
    public void setEventTime(String eventTime) { this.eventTime = eventTime; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOrganizer() { return organizer; }
    public void setOrganizer(String organizer) { this.organizer = organizer; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getExpectedAttendees() { return expectedAttendees; }
    public void setExpectedAttendees(int expectedAttendees) { this.expectedAttendees = expectedAttendees; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return title + " @ " + venue;
    }
}
