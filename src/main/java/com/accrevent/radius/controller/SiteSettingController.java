package com.accrevent.radius.controller;

import com.accrevent.radius.dto.SiteSettingsDTO;
import com.accrevent.radius.model.SiteSettings;
import com.accrevent.radius.repository.SiteSettingsRepository;
import com.accrevent.radius.service.SiteSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/SiteSettings")
public class SiteSettingController {


    private final SiteSettingService siteSettingService;
    private final SiteSettingsRepository siteSettingsRepository;

    public SiteSettingController(SiteSettingService siteSettingService, SiteSettingsRepository siteSettingsRepository) {
        this.siteSettingService = siteSettingService;
        this.siteSettingsRepository = siteSettingsRepository;
    }

    @PostMapping("/createSiteSettings")
    public ResponseEntity<?> createSiteSettings(@RequestBody SiteSettingsDTO dto) {
       try{
           SiteSettings saved = siteSettingService.createSiteSetting(dto);

           SiteSettingsDTO response = new SiteSettingsDTO();
           response.setInternalName(saved.getInternalName());
           response.setValue(saved.getValue());

           return ResponseEntity.ok(response);

       } catch (IllegalArgumentException e) {
           return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
       }
    }


    @GetMapping("/getAllSiteSettings")
    public ResponseEntity<Map<String, String>> getAllSiteSettings() {
        Map<String, String> settingsMap = siteSettingService.getAllSiteSettings()
                .stream()
                .collect(Collectors.toMap(SiteSettings::getInternalName, SiteSettings::getValue));

        return ResponseEntity.ok(settingsMap);
    }


    @GetMapping("/getSiteSettings")
    public ResponseEntity<SiteSettingsDTO> getSiteSettings(@RequestParam String internalName) {
        SiteSettings setting = siteSettingService.getSiteSetting(internalName);

        SiteSettingsDTO response = new SiteSettingsDTO();
        response.setInternalName(setting.getInternalName());
        response.setValue(setting.getValue());

        return ResponseEntity.ok(response);
    }


    @PutMapping("/updateSiteSettings")
    public ResponseEntity<Map<String, String>> updateSiteSettings(@RequestBody Map<String, String> settings) {
        try {
            Map<String, String> updated = siteSettingService.updateSiteSettings(settings);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.unprocessableEntity().body(error); // 422
        }
    }


}
