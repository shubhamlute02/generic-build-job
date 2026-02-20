package com.accrevent.radius.repository;

import com.accrevent.radius.model.SiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteSettingsRepository extends JpaRepository<SiteSettings,Long> {
    Optional<SiteSettings> findByInternalName(String internalName);

}
