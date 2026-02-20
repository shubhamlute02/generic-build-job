package com.accrevent.radius.controller;

//import com.accrevent.radius.model.ConstantBusinessUnit;
//import com.accrevent.radius.repository.ConstantBusinessUnitRepository;
import com.accrevent.radius.service.BusinessUnitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/constant")

public class ConstantController {
    private static final String SUCCESS_STATUS = "success";
    private static final String ERROR_STATUS = "failure";

    @Autowired
    private BusinessUnitService businessUnitService;

    @GetMapping("/getBusinessUnit")
    public ResponseEntity<Map<String, Object>> getAllBusinessUnits() {
        Map<String, Object> responseBody = new HashMap<>();
        try {
            List<String> businessUnits = businessUnitService.getAllBusinessUnits();
            if (businessUnits.isEmpty()) {
                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "No Business Units Exist");
            }
            responseBody.put("businessUnits", businessUnits);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Business units retrieved successfully", responseBody);
        } catch(Exception e) {
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_STATUS, e.getMessage());
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
