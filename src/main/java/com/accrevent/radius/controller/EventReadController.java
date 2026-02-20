package com.accrevent.radius.controller;


import com.accrevent.radius.dto.EventDTO;
import com.accrevent.radius.service.CalendarService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/calendar")
public class EventReadController {

    private final CalendarService calendarService;
    private static final String SUCCESS_STATUS = "success";
    private static final String ERROR_STATUS = "failure";

    public EventReadController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping("/events")
    public ResponseEntity<Map<String,Object>> getEvents(@RequestParam String emailId,
                                         @RequestParam Long startDateTime, @RequestParam Long endDateTime) {
        List<EventDTO> stringObjectMap = calendarService.getCalendarEvents(emailId,startDateTime,endDateTime);

        try {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("Event List", stringObjectMap);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "The Event list was received successfully.", responseBody);
        }catch(Exception e)
        {
            return buildResponse(HttpStatus.NOT_FOUND, ERROR_STATUS, e.getMessage());
        }
    }

    public ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String responseStatus, String message) {
        return buildResponse(status, responseStatus, message, new HashMap<>());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String responseStatus, String message, Map<String, Object> additionalData) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", responseStatus);
        response.put("message", message);
        response.putAll(additionalData);
        return ResponseEntity.status(status).body(response);
    }
}
