package com.accrevent.radius.controller;

import com.accrevent.radius.dto.MarketingStoryDTO;
import com.accrevent.radius.dto.VersionDTO;
import com.accrevent.radius.service.MarketingStoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/marketingStory")
public class MarketingStoryController {

    private final MarketingStoryService marketingStoryService;

    public MarketingStoryController(MarketingStoryService marketingStoryService) {
        this.marketingStoryService = marketingStoryService;
    }

    @PostMapping("/createMarketingStory")
    public ResponseEntity<?> createMarketingStory(@RequestBody MarketingStoryDTO marketingStoryDTO){
        try {
            MarketingStoryDTO createdStory = marketingStoryService.createMarketingStory(marketingStoryDTO);
            return ResponseEntity.ok(createdStory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create Marketing Story: " + e.getMessage());
        }
    }


    @GetMapping("/getMarketingStoryDetailsById")
    public ResponseEntity<?> getMarketingStoryDetails(@RequestParam Long id) {
        try {
            MarketingStoryDTO storyDTO = marketingStoryService.getMarketingStoryDetails(id);
            if (storyDTO == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(storyDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch Marketing Story: " + e.getMessage());
        }
    }


    @PutMapping("/updateMarketingStory")
    public ResponseEntity<?> updateMarketingStory(@RequestParam Long id,
                                                  @RequestBody MarketingStoryDTO marketingStoryDTO) {
        try {
            MarketingStoryDTO updatedStory = marketingStoryService.updateMarketingStory(id, marketingStoryDTO);
            return ResponseEntity.ok(updatedStory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update Marketing Story: " + e.getMessage());
        }
    }

    @GetMapping("/getInWorkMarketingStoriesByCollection")
    public ResponseEntity<?> getInWorkMarketingStories(@RequestParam Long marketingCollectionId) {
        try {
            Map<String, Object> response =
                    marketingStoryService.getInWorkMarketingStoriesByCollection(marketingCollectionId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("Invalid input: " + ex.getMessage()); // 400 for bad request
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching in-work marketing stories: " + ex.getMessage()); // 500 for unexpected errors
        }
    }




    @DeleteMapping("/deleteMarketingStory")
    public ResponseEntity<String> deleteMarketingStory(@RequestParam Long id) {
        try {
            marketingStoryService.deleteMarketingStory(id);
            return ResponseEntity.ok("Marketing Story deleted successfully with ID: " + id);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete Marketing Story: " + e.getMessage());
        }
    }


    @GetMapping("/getMarketingStoriesByCollectionId")
    public ResponseEntity<Map<String, Object>> getMarketingStoriesByCollectionId(
            @RequestParam Long marketingCollectionId) {

        // Fetch stories by department internalName
        List<MarketingStoryDTO> stories = marketingStoryService.getMarketingStoriesByCollectionId(marketingCollectionId);

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("marketingStories", stories);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/getInWorkAndLatestApprovedMarketingStoriesByCollectionId")
    public ResponseEntity<Map<String, Object>> getInWorkAndLatestApprovedMarketingStoriesByCollectionId(
            @RequestParam Long marketingCollectionId) {

        // Fetch stories by department internalName
        Map<String, Object> stories = marketingStoryService.getInWorkAndLatestApprovedMarketingStoriesByCollectionId(marketingCollectionId);

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("marketingStories", stories);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/getContentConsumers")
    public ResponseEntity<Map<String, Object>> getContentConsumers() {
        Map<String, Object> responseBody = new HashMap<>();
        try {
            List<String> getContentConsumers = marketingStoryService.getContentConsumers();
            responseBody.put("getContentConsumers", getContentConsumers);
            return ResponseEntity.ok(responseBody);
        } catch(Exception e) {
            responseBody.put("error", "Failed to fetch getContentConsumers: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBody);
        }
    }



}
