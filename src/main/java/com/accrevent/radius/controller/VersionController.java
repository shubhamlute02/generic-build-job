package com.accrevent.radius.controller;

import com.accrevent.radius.dto.LifecycleStatusUpdateDTO;
import com.accrevent.radius.service.LifecycleService;
import com.accrevent.radius.service.VersionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.accrevent.radius.dto.VersionDTO;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/version")
public class VersionController {

    private final VersionService versionService;
    private final LifecycleService lifecycleService;

    public VersionController(VersionService versionService, LifecycleService lifecycleService) {
        this.versionService = versionService;
        this.lifecycleService = lifecycleService;
    }

    @PostMapping("/createVersion")
    public ResponseEntity<?> createVersion(@RequestParam Long editionId, @RequestBody VersionDTO versionDTO) {
      try{

          VersionDTO createdVersion = versionService.createVersion(editionId,versionDTO);
          return ResponseEntity.ok(createdVersion);

      } catch (Exception e) {
          {
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(e.getMessage());

          }
      }
    }


    @PutMapping("/updateMarketingStoryVersion")
    public ResponseEntity<?> updateMarketingStoryVersion(@RequestBody VersionDTO versionDTO) {
       try{
           VersionDTO updated = versionService.updateVersion(versionDTO);
           return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
           return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
       }
    }

    @DeleteMapping("/deleteMarketingStoryVersion")
    public ResponseEntity<String> deleteMarketingStoryVersion(@RequestParam Long versionId) {
        try {
            versionService.deleteVersion(versionId);
            return ResponseEntity.ok("Version deleted successfully with id: " + versionId);
        }  catch (Exception e) {
            // Catch unexpected exceptions
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete version with id " + versionId + ". Reason: " + e.getMessage());
        }
    }



    @GetMapping("/getVersionByEditionId")
    public ResponseEntity<Map<String, List<VersionDTO>>> getVersionByEditionId(@RequestParam Long editionId) {
        List<VersionDTO> versions = versionService.getVersionsByEditionId(editionId);

        Map<String, List<VersionDTO>> response = new HashMap<>();
        response.put("versions", versions);

        return ResponseEntity.ok(response);
    }


}
