package com.accrevent.radius.service;

import com.accrevent.radius.dto.SiteSettingsDTO;
import com.accrevent.radius.model.SiteSettings;
import com.accrevent.radius.repository.SiteSettingsRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SiteSettingService {

    private final SiteSettingsRepository siteSettingsRepository;

    public SiteSettingService(SiteSettingsRepository siteSettingsRepository) {
        this.siteSettingsRepository = siteSettingsRepository;
    }

    public SiteSettings createSiteSetting(SiteSettingsDTO dto) {
        // Prevent duplicate internalName
        siteSettingsRepository.findByInternalName(dto.getInternalName())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Setting with this internalName already exists");
                });

        SiteSettings setting = new SiteSettings();
        setting.setInternalName(dto.getInternalName());
        setting.setValue(dto.getValue());
        return siteSettingsRepository.save(setting);
    }

    public List<SiteSettings> getAllSiteSettings() {
        return siteSettingsRepository.findAll();
    }

    public SiteSettings getSiteSetting(String internalName) {
        return siteSettingsRepository.findByInternalName(internalName)
                .orElseThrow(() -> new IllegalArgumentException("No setting found for internalName: " + internalName));
    }

    @Transactional
    public Map<String, String> updateSiteSettings(Map<String, String> settings) {
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, String> entry : settings.entrySet()) {
            Optional<SiteSettings> existing = siteSettingsRepository.findByInternalName(entry.getKey());

            if (existing.isPresent()) {
                SiteSettings setting = existing.get();
                setting.setValue(entry.getValue());
                siteSettingsRepository.save(setting);
                result.put(entry.getKey(), entry.getValue());
            } else {
                // throw a standard exception
                throw new IllegalArgumentException("Setting with internalName '" + entry.getKey() + "' not found");
            }
        }

        return result;
    }


}
