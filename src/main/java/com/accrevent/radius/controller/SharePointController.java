package com.accrevent.radius.controller;

import com.accrevent.radius.dto.SharePointURLDTO;
import com.accrevent.radius.service.SharePointService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sharepoint")
public class SharePointController {

    private final SharePointService sharePointService;


    public SharePointController(SharePointService sharePointService) {
        this.sharePointService = sharePointService;

    }


    @PostMapping("/createFolder")
    public ResponseEntity<SharePointURLDTO> createFolder(
            @RequestParam String parentUrl,
            @RequestParam String folderName) {

        SharePointURLDTO response = sharePointService.createFolderIfNotPresent(parentUrl, new String[]{folderName});
        return ResponseEntity.ok(response);
    }

}
