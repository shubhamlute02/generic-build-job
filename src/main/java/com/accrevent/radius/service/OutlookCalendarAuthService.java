package com.accrevent.radius.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OutlookCalendarAuthService {

    private final String tokenEndpoint = "https://login.microsoftonline.com/" + "/oauth2/v2.0/token";

    public String getAccessToken() {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_id=" + 
                "&client_secret=" + 
                "&scope=https%3A%2F%2Fgraph.microsoft.com%2F.default" +
                "&grant_type=client_credentials";

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(tokenEndpoint, HttpMethod.POST, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        } else {
            throw new RuntimeException("Failed to fetch access token");
        }
    }
}
