package com.accrevent.radius.controller;


import com.accrevent.radius.dto.UserRegionDTO;
import com.accrevent.radius.service.UserRegionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/useRegion")

public class UserRegionController {
    private static final Logger logger = LoggerFactory.getLogger(UserRegionController.class);

    private static final String SUCCESS_STATUS = "success";
    private static final String ERROR_STATUS = "failure";
   
    private final UserRegionService userRegionService;

    public UserRegionController(UserRegionService userRegionService)
    {
        this.userRegionService = userRegionService;
    }

    @GetMapping("/getAll")
    public ResponseEntity<Map<String, Object>> getAllUserRegion()
    {
        Map<String, Object> responseBody = new HashMap<>();
        try
        {
            List<UserRegionDTO> userRegionlist = userRegionService.getAllUserRegion();
            if (userRegionlist.isEmpty()) {
                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "No userRegion Exist");
            }
            responseBody.put("UserRegion", userRegionlist);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "get all userRegion successfully.", responseBody);
        }
        catch(Exception e)
        {
            return buildResponse(HttpStatus.NOT_FOUND, ERROR_STATUS, e.getMessage());
        }
    }


    @PutMapping("/add")
    public ResponseEntity<Map<String,Object>> createUserRegion(@RequestBody UserRegionDTO userRegionDTO)
    {
        try
        {
            UserRegionDTO createdUserRegion = userRegionService.createUserRegion(userRegionDTO);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("createdUserRegion", createdUserRegion);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "UserRegion successfully created.", responseBody);
        }
        catch(Exception e)
        {
            return buildResponse(HttpStatus.BAD_REQUEST,ERROR_STATUS,e.getMessage());
        }
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<Map<String,Object>> deleteUserRegion(@RequestParam Long userRegionId){

        try {
            boolean isDeleted = userRegionService.deleteUserRegion(userRegionId);
            if (isDeleted) {
                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, userRegionId + " UserRegion successfully deleted."); // 200 OK with response body
            } else {
                logger.warn("Failed to delete userRegion with ID: {}", userRegionId);
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_STATUS, "Failed to delete the UserRegion.");
            }
        }catch(Exception e)
        {
            logger.warn("Deleting purpose given UserRegion ID: {} not found", userRegionId);
            return buildResponse(HttpStatus.NOT_FOUND,ERROR_STATUS,"UserRegion not found."); // 404 Not Found with response body

        }
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
