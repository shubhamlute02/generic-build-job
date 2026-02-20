package com.accrevent.radius.repository;

import com.accrevent.radius.model.OutreachCampaignMaturityRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OutreachCampaignMaturityRecordRepository
        extends JpaRepository<OutreachCampaignMaturityRecord, Long> {

    Optional<OutreachCampaignMaturityRecord> findByCampaignIdAndDateAndStatus(
            Long campaignId, Long date, String status);

    List<OutreachCampaignMaturityRecord> findByCampaignIdOrderByDateAsc(Long campaignId);

    List<OutreachCampaignMaturityRecord> findByStatus(String status);

}
