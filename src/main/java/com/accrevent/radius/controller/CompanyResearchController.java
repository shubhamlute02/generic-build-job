package com.accrevent.radius.controller;

import com.accrevent.radius.dto.CompanyResearchDTO;
import com.accrevent.radius.dto.UserResearchSequenceDTO;
import com.accrevent.radius.service.CompanyResearchSequenceService;
import com.accrevent.radius.service.CompanyResearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/companyResearch")
public class CompanyResearchController {

    private static final String SUCCESS_STATUS = "success";
    private static final String ERROR_STATUS = "error";
    private static final String COMPANY_ID_REQUIRED = "Company ID is required";

    private final CompanyResearchService companyResearchService;
    private final CompanyResearchSequenceService companyResearchSequenceService;
    @Autowired
    private ObjectMapper objectMapper;

    public CompanyResearchController(CompanyResearchService companyResearchService, CompanyResearchSequenceService companyResearchSequenceService) {
        this.companyResearchService = companyResearchService;
        this.companyResearchSequenceService = companyResearchSequenceService;
    }


    @GetMapping("/getRevenue")
    public ResponseEntity<List<String>> getRevenueOptions() {
        List<String> revenueList = companyResearchService.getCompanyRevenue();
        return ResponseEntity.ok(revenueList);
    }

    @GetMapping("/getEmployeeCount")
    public ResponseEntity<List<String>> getEmployeeCountOptions() {
        List<String> employeeCountList = companyResearchService.getCompanyEmployeeCount();
        return ResponseEntity.ok(employeeCountList);
    }

    @PostMapping("/addCompanyResearch")
    public ResponseEntity<CompanyResearchDTO> addCompanyResearch(@RequestBody Map<String, Object> request) {
        try {
            Long companyId = Long.valueOf(request.get("companyId").toString());

            ObjectMapper mapper = new ObjectMapper();
            CompanyResearchDTO researchDTO = mapper.convertValue(request.get("companyResearch"), CompanyResearchDTO.class);
            researchDTO.setCompanyId(companyId);

            CompanyResearchDTO savedDTO = companyResearchService.addCompanyResearch(researchDTO);

            return ResponseEntity.ok(savedDTO);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to add research: " + e.getMessage(), e);
        }
    }


    @PutMapping("/updateCompanyResearch")
    public ResponseEntity<Map<String, Object>> updateCompanyResearch(@RequestBody Map<String, Object> request) {
        Long companyId = Long.valueOf(request.get("companyId").toString());

        ObjectMapper mapper = new ObjectMapper();
        CompanyResearchDTO researchDTO = mapper.convertValue(request.get("companyResearch"), CompanyResearchDTO.class);
        researchDTO.setCompanyId(companyId);

        CompanyResearchDTO updated = companyResearchService.updateCompanyResearch(researchDTO);

        // Prepare nested JSON response
        Map<String, Object> response = new HashMap<>();
        response.put("companyId", updated.getCompanyId());
        response.put("companyResearch", updated);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/deleteCompanyResearch")
    public ResponseEntity<String> deleteCompanyResearch(@RequestParam Long companyResearchId) {
        companyResearchService.deleteCompanyResearch(companyResearchId);
        return ResponseEntity.ok("Company Research deleted successfully");
    }


    @GetMapping("/getCompanyResearchDetails")
    public ResponseEntity<Map<String, Object>> getCompanyResearchDetails(@RequestParam Long companyId) {
        try {
            CompanyResearchDTO researchDetails = companyResearchService.getCompanyResearchDetails(companyId);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("researchDetails", researchDetails);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Company research details retrieved successfully", responseBody);
        } catch (Exception e) {
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, e.getMessage());
        }


    }

    @PostMapping("/addResearchSequence")
    public ResponseEntity<Map<String, Object>> addResearchSequence(
            @RequestBody List<UserResearchSequenceDTO> sequences) {

        List<UserResearchSequenceDTO> savedSequences = companyResearchSequenceService.addSequences(sequences);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sequences added successfully");
        response.put("data", savedSequences);

        return ResponseEntity.ok(response);
    }


    @PutMapping("/updateResearchSequence")
    public ResponseEntity<Map<String, Object>> updateResearchSequence(@RequestBody Map<String, List<UserResearchSequenceDTO>> request)
    {

        List<UserResearchSequenceDTO> updatedList = companyResearchSequenceService.updateResearchSequence(
                request.get("userResearchSequence")
        );

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("userResearchSequence", updatedList);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "User Research sequence updated successfully");
        response.put("data", responseData);

        return ResponseEntity.ok(response);
    }


    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String responseStatus, String message) {
        return buildResponse(status, responseStatus, message, null);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String responseStatus, String message, Map<String, Object> responseBody) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", responseStatus);
        response.put("message", message);

        if (responseBody != null) {
            response.putAll(responseBody);
        }

        return new ResponseEntity<>(response, status);
    }
}