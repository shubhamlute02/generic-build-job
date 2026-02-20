package com.accrevent.radius.service;


import com.accrevent.radius.dto.EventDTO;
import com.accrevent.radius.exception.ResourceNotFoundException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.accrevent.radius.util.RadiusUtil.longToZonedDateTime;
import static com.accrevent.radius.util.RadiusUtil.stringtoZonedDateTime;

@Service
public class CalendarService {

    private final OutlookCalendarAuthService authService;
    private static final DateTimeFormatter AppInputOutputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter apiInputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final DateTimeFormatter apiOutputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS");
    public CalendarService(OutlookCalendarAuthService authService) {
        this.authService = authService;
    }

    public List<EventDTO> getCalendarEvents(String userId, Long startDateTime, Long endDateTime) {
        RestTemplate restTemplate = new RestTemplate();
        String accessToken = authService.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ZonedDateTime startTime = longToZonedDateTime(startDateTime);
        ZonedDateTime endTime = longToZonedDateTime(endDateTime);
        String url = "https://graph.microsoft.com/v1.0/users/" + userId + "/calendarView"
                + "?startDateTime=" + startTime.format(apiInputFormatter) + "&endDateTime=" + endTime.format(apiInputFormatter);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);


        if (response.getStatusCode() == HttpStatus.OK) {
            JSONObject jsonObject = new JSONObject(response.getBody());
            JSONArray jsonArray = (JSONArray) jsonObject.get("value");
            List<EventDTO> eventDTOList = new ArrayList<>();
            int arrLength =jsonArray.length();
                for(int i =0 ; i < arrLength;i++) {
                    EventDTO eventDTO = new EventDTO();
                    JSONObject jsonValue = (JSONObject) jsonArray.get(i);
                    eventDTO.setSubject(jsonValue.get("subject").toString());

                    JSONObject jsonOrganizerObj = (JSONObject) jsonValue.get("organizer");
                    JSONObject jsonOrganizerEmailAddress = (JSONObject) jsonOrganizerObj.get("emailAddress");
                    eventDTO.setEventOrganizer(jsonOrganizerEmailAddress.get("name").toString());
                    JSONObject jsonStartDateObj = (JSONObject) jsonValue.get("start");
                    JSONObject jsonEndDateObj = (JSONObject) jsonValue.get("end");

                    eventDTO.setStartDate(apiTimeConvertIntoRequireLongFormat(
                            jsonStartDateObj.get("dateTime").toString()));
                    eventDTO.setEndDate(apiTimeConvertIntoRequireLongFormat(
                            jsonEndDateObj.get("dateTime").toString()));
                    eventDTOList.add(eventDTO);
                }

            return eventDTOList;
        } else {
            throw new RuntimeException("Failed to fetch calendar events");
        }
    }


    private String apiTimeConvertIntoRequireStringFormat(String dateStr){
            // Parse the string into a LocalDateTime
            LocalDateTime localDateTime = LocalDateTime.parse(dateStr, apiOutputFormatter);
            // Convert the LocalDateTime to ZonedDateTime in UTC
            ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of("UTC"));
            return zonedDateTime.format(AppInputOutputFormatter);
    }
    private long apiTimeConvertIntoRequireLongFormat(String dateStr) {
        // Parse the string into a LocalDateTime
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, apiOutputFormatter);

        // Convert the LocalDateTime to ZonedDateTime in UTC
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of("UTC"));

        // Return the time as milliseconds since epoch
        return zonedDateTime.toInstant().toEpochMilli();
    }
}


