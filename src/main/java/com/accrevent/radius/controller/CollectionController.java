package com.accrevent.radius.controller;

import com.accrevent.radius.dto.CollectionDTO;
import com.accrevent.radius.service.CollectionService;
import com.accrevent.radius.service.MarketingStoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/collection")
public class CollectionController {

    private final MarketingStoryService marketingStoryService;
    private final CollectionService collectionService;
    public CollectionController(MarketingStoryService marketingStoryService, CollectionService collectionService) {
        this.marketingStoryService = marketingStoryService;
        this.collectionService = collectionService;
    }

    @PostMapping("/createCollection")
    public ResponseEntity<?>createCollection(@RequestBody CollectionDTO collectionDTO){
        Map<String, Object>response = new HashMap<>();
        CollectionDTO saved = collectionService.createCollection(collectionDTO);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/getMarketingStoryCollection")
    public Map<String, List<CollectionDTO>> getMarketingStoryCollections() {
        List<CollectionDTO> collections = collectionService.getAllCollections();
        Map<String, List<CollectionDTO>> response = new HashMap<>();
        response.put("marketingStoryCollections", collections);
        return response;
    }





}
