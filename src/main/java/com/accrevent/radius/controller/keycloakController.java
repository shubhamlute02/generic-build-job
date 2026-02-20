package com.accrevent.radius.controller;

import com.accrevent.radius.service.KeycloakService;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/keycloak")
public class keycloakController {
    private static final Logger logger = LoggerFactory.getLogger(keycloakController.class);

    private static final String SUCCESS_STATUS = "success";
    private static final String ERROR_STATUS = "failure";

    private final KeycloakService keycloakService;

    public keycloakController(KeycloakService keycloakService)
    {
        this.keycloakService = keycloakService;
    }

    @GetMapping("/getUsers")
    public ResponseEntity<Map<String,Object>> getKeycloakUsers()
    {
        try
        {
            List<Map<String, Object>> Users = keycloakService.getAllUsers();
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("Users", Users);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Got the all users successfully.", responseBody);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return buildResponse(HttpStatus.BAD_REQUEST,ERROR_STATUS,e.getMessage());
        }
    }

    // Logout API by username (can adapt to token or principal if needed)
    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(@RequestParam String username) {
        Optional<String> userIdOpt = keycloakService.getUserIdByUsername(username);
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }
        keycloakService.logoutUserByUserId(userIdOpt.get());
        return ResponseEntity.ok("User logged out successfully");
    }



    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String responseStatus, String message) {
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
