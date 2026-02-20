package com.accrevent.radius.controller;

import com.accrevent.radius.dto.EditionDTO;
import com.accrevent.radius.dto.EditionRequestDTO;
import com.accrevent.radius.service.EditionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/edition")
public class EditionController {

    private final EditionService editionService;

    public EditionController(EditionService editionService) {
        this.editionService = editionService;
    }

    @PostMapping("/createEdition")
    public ResponseEntity<EditionDTO> createEdition(
            @RequestBody EditionRequestDTO request) {
        EditionDTO response = editionService.createEdition(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/updateEdition")
    public ResponseEntity<EditionDTO> updateEdition(@RequestBody EditionRequestDTO editionRequestDTO) {
        EditionDTO updatedEdition = editionService.updateEdition(editionRequestDTO);
        return ResponseEntity.ok(updatedEdition);
    }

    @DeleteMapping("/deleteEdition")
    public ResponseEntity<String> deleteEdition(@RequestParam Long editionId) {
        try{
            editionService.deleteEdition(editionId);
            return ResponseEntity.ok("Edition with ID " + editionId + " deleted successfully.");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete Edtion " + e.getMessage());
        }
    }

    @GetMapping("/getContentTypes")
    public ResponseEntity<Map<String, Object>> getContentTypes() {
        Map<String, Object> responseBody = new HashMap<>();
        try {
            List<String> contentTypes = editionService.getContentTypes();
            responseBody.put("contentTypes", contentTypes);
            return ResponseEntity.ok(responseBody);
        } catch(Exception e) {
            responseBody.put("error", "Failed to fetch content types: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBody);
        }
    }
}
