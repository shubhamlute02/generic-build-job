package com.accrevent.radius.controller;

import com.accrevent.radius.dto.CompanyDTO;
import com.accrevent.radius.service.CompanyService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/company")
public class    CompanyController {

    private static final String SUCCESS_STATUS = "success";
    private static final String ERROR_STATUS = "error";
    private static final String COMPANY_NAME_REQUIRED = "Company name is required";

    private final CompanyService companyService;
    private final Logger logger = LoggerFactory.getLogger(CompanyController.class);

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> createCompany(@RequestBody CompanyDTO companyDTO) {
        try {
            if (companyDTO.getName() == null || companyDTO.getName().isEmpty()) {
                logger.error(COMPANY_NAME_REQUIRED);
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, COMPANY_NAME_REQUIRED);
            }

            CompanyDTO createdCompany = companyService.createCompany(companyDTO);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("company", createdCompany);

            return buildResponse(HttpStatus.CREATED, SUCCESS_STATUS, "Company successfully created", responseBody);
        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, e.getMessage());
        }
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> updateCompany(
            @RequestParam Long companyId,
            @RequestBody CompanyDTO companyDTO) {
        try {
            if (companyDTO.getName() == null || companyDTO.getName().isEmpty()) {
                logger.error(COMPANY_NAME_REQUIRED);
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, COMPANY_NAME_REQUIRED);
            }

            companyDTO.setCompanyId(companyId);
            CompanyDTO updatedCompany = companyService.updateCompany(companyDTO);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("company", updatedCompany);

            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Company successfully updated", responseBody);
        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, e.getMessage());
        }
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<Map<String, Object>> deleteCompany(@RequestParam Long companyId) {
        try {
            companyService.deleteCompany(companyId);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Company successfully deleted");
        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, e.getMessage());
        }
    }


    @GetMapping("/getAllCompanies")
    public ResponseEntity<Map<String, Object>> getAllCompanies() {
        try {
            List<CompanyDTO> companies = companyService.getAllCompanies();
            Map<String, Object> response = new HashMap<>();

            response.put("status", SUCCESS_STATUS);
            response.put("message", "Get all companies successfully");
            response.put("data", companies); // This will include all companies with their research

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, e.getMessage());
        }
    }

    @GetMapping("/getCompanyById")
    public ResponseEntity<Map<String, Object>> getCompanyById(@RequestParam Long companyId) {
        try {
            CompanyDTO companyDTO = companyService.getCompanyById(companyId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", SUCCESS_STATUS);
            response.put("message", "Company retrieved successfully");

            Map<String, Object> data = new HashMap<>();
            data.put("companyId", companyDTO.getCompanyId());
            data.put("companyResearch", companyDTO.getCompanyResearch());

            response.put("data", data);

            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return buildResponse(HttpStatus.NOT_FOUND, ERROR_STATUS, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_STATUS, "Error retrieving company");
        }
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String responseStatus, String message) {
        return buildResponse(status, responseStatus, message, null);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String responseStatus,
            String message,
            Map<String, Object> responseBody) {

        Map<String, Object> response = new HashMap<>();
        response.put("status", responseStatus);
        response.put("message", message);

        if (responseBody != null) {
            response.putAll(responseBody);
        }

        return new ResponseEntity<>(response, status);
    }

}
