package com.accrevent.radius.controller;

import com.accrevent.radius.dto.ResearchTopicDTO;
import com.accrevent.radius.service.ResearchTopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/researchTopic")
public class ResearchTopicController {
    @Autowired
    private ResearchTopicService researchTopicService;

    @PostMapping("/addTopic")
    public ResponseEntity<Map<String, Object>> createResearchTopic(
            @RequestParam Long companyResearchId,
            @RequestBody ResearchTopicDTO dto) {
        Map<String, Object> response = new HashMap<>();
      try{
          ResearchTopicDTO saved = researchTopicService.createResearchTopic(companyResearchId, dto);


          response.put("status", "success");
          response.put("message", "Research created successfully");
          response.put("data", saved);

          return ResponseEntity.ok(response);
      } catch (RuntimeException ex) {
          response.put("status", "error");
          response.put("message", ex.getMessage());
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
      } catch (Exception e) {
          response.put("status", "error");
          response.put("message", "Failed to create research topic: " + e.getMessage());
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
      }
    }

    @PutMapping("/updateTopic")
    public ResponseEntity<Map<String, Object>> updateResearchTopic(
            @RequestBody ResearchTopicDTO dto) {

        Map<String, Object> response = new HashMap<>();
        try{
            ResearchTopicDTO updated = researchTopicService.updateResearchTopic(dto);

            response.put("status", "success");
            response.put("message", "Research topic updated successfully");
            response.put("data", updated);

            return ResponseEntity.ok(response);
        }catch (RuntimeException ex) {
            response.put("status", "error");
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

    }

    @DeleteMapping("/deleteTopic")
    public ResponseEntity<Map<String, Object>> deleteResearchTopic(@RequestParam Long topicId) {
        researchTopicService.deleteResearchTopic(topicId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Research topic deleted successfully");

        return ResponseEntity.ok(response);
    }
}
