package com.accrevent.radius.repository;

import com.accrevent.radius.model.EmailOutreachCampaignMaturityRecordRegister;
import com.accrevent.radius.model.LinkedInOutreachCampaignMaturityRecordRegister;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LinkedInOutreachCampaignMaturityRecordRegisterRepository extends JpaRepository<LinkedInOutreachCampaignMaturityRecordRegister, Long> {

    Optional<LinkedInOutreachCampaignMaturityRecordRegister> findByCampaignIdAndDateAndStatus(
            Long campaignId, Long date, String status);

    List<LinkedInOutreachCampaignMaturityRecordRegister> findByCampaignIdOrderByDateAsc(Long campaignId);

    List<LinkedInOutreachCampaignMaturityRecordRegister> findByStatus(String status);

}
