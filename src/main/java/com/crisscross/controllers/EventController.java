package com.crisscross.controllers;

import com.crisscross.dao.EventDAO;
import com.crisscross.models.Event;
import com.crisscross.ui.ScreenManager;

import java.util.List;

public class EventController {
    private final EventDAO eventDAO = new EventDAO();
    private final ScreenManager screenManager = ScreenManager.getInstance();

    public List<Event> getAllEvents() {
        return eventDAO.findAll();
    }

    public List<Event> getUpcomingEvents() {
        return eventDAO.findUpcoming();
    }

    public void selectEventForBooking(Event event) {
        screenManager.showEventDetails(event);
    }

    public void proceedToVehicleSelection(Event event) {
        screenManager.showVehicleSelection(event);
    }

    public boolean createEvent(Event event) {
        return eventDAO.insert(event);
    }

    public boolean updateEvent(Event event) {
        return eventDAO.update(event);
    }

    public boolean deleteEvent(int eventId) {
        return eventDAO.delete(eventId);
    }
}
