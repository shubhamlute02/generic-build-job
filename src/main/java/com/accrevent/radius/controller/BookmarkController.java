package com.accrevent.radius.controller;

import com.accrevent.radius.dto.BookmarkDTO;
import com.accrevent.radius.service.BookmarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookmark")
public class BookmarkController {

    @Autowired
    private BookmarkService bookmarkService;

    @PostMapping("/saveBookmarkForLead")
    public ResponseEntity<String> saveBookmarkForLead(@RequestBody BookmarkDTO bookmarkDTO) {
        bookmarkService.saveBookmarkForLead(bookmarkDTO.getUserName(), bookmarkDTO.getLeadId(), bookmarkDTO.getIsBookmarked());
        return ResponseEntity.ok("Bookmark saved for lead");
    }

    @PostMapping("/saveBookmarkForOpportunity")
    public ResponseEntity<String> saveBookmarkForOpportunity(@RequestBody BookmarkDTO bookmarkDTO) {
        bookmarkService.saveBookmarkForOpportunity(bookmarkDTO.getUserName(), bookmarkDTO.getOpportunityId(), bookmarkDTO.getIsBookmarked());
        return ResponseEntity.ok("Bookmark saved for opportunity");
    }

    @GetMapping("/getBookmarkForLead")
    public ResponseEntity<Boolean> getBookmarkForLead(@RequestParam String userName, @RequestParam Long leadId) {
        Boolean isBookmarked = bookmarkService.getBookmarkForLead(userName, leadId);
        return ResponseEntity.ok(isBookmarked);
    }

    @GetMapping("/getBookmarkForOpportunity")
    public ResponseEntity<Boolean> getBookmarkForOpportunity(@RequestParam String userName, @RequestParam Long opportunityId) {
        Boolean isBookmarked = bookmarkService.getBookmarkForOpportunity(userName, opportunityId);
        return ResponseEntity.ok(isBookmarked);
    }
}

