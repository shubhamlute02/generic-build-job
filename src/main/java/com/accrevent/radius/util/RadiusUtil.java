package com.accrevent.radius.util;

import com.accrevent.radius.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class RadiusUtil {
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter COMMENT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("E dd-MMM-yyyy");  // e.g., Thu 19-Jun-2025

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthToken) {
            Jwt jwt = jwtAuthToken.getToken();

            String firstName = jwt.getClaim("given_name");
            String lastName = jwt.getClaim("family_name");

            if (firstName != null || lastName != null) {
                String fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                return fullName.trim();
            }

            return jwt.getClaim("preferred_username"); // fallback if names not available
        }
        return "Unknown User"; // fallback case
    }

//    public static ZonedDateTime stringtoZonedDateTime(String dateStr){
//        try {
//            // Parse the string into a LocalDateTime
//            LocalDateTime localDateTime = LocalDateTime.parse(dateStr, formatter);
//            // Convert the LocalDateTime to ZonedDateTime in UTC
//            return localDateTime.atZone(ZoneId.of("UTC"));
//        }
//        catch (DateTimeParseException e)
//        {
//            throw new ResourceNotFoundException("Invalid date format: " + dateStr+" Error="+e.getMessage());
//        }
//        catch (Exception e) {
//            throw new ResourceNotFoundException("An unexpected error occurred while processing the date: " + dateStr+" Error="+ e.getMessage());
//        }
//    }

    public static ZonedDateTime stringtoZonedDateTime(String dateStr) {
        try {
            // Convert the string to long
            long timestamp = Long.parseLong(dateStr);

            // Convert the timestamp to Instant
            Instant instant = Instant.ofEpochMilli(timestamp);

            // Convert the Instant to ZonedDateTime in UTC
            return instant.atZone(ZoneId.of("UTC"));
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException("Invalid timestamp format: " + dateStr + " Error=" + e.getMessage());
        } catch (Exception e) {
            throw new ResourceNotFoundException("An unexpected error occurred while processing the timestamp: " + dateStr + " Error=" + e.getMessage());
        }
    }

    public static ZonedDateTime longToZonedDateTime(long timestamp) {
        try {
            // Convert the timestamp (in milliseconds) to Instant
            Instant instant = Instant.ofEpochMilli(timestamp);

            // Convert the Instant to ZonedDateTime in UTC
            return instant.atZone(ZoneId.of("UTC"));
        } catch (Exception e) {
            throw new ResourceNotFoundException("Error while converting timestamp to ZonedDateTime: " + e.getMessage());
        }
    }
}
