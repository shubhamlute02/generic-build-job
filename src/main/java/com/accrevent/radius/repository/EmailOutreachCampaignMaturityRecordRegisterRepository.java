package com.accrevent.radius.repository;

import com.accrevent.radius.model.EmailOutreachCampaignMaturityRecordRegister;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailOutreachCampaignMaturityRecordRegisterRepository
        extends JpaRepository<EmailOutreachCampaignMaturityRecordRegister, Long> {

    Optional<EmailOutreachCampaignMaturityRecordRegister> findByCampaignIdAndDateAndStatus(
            Long campaignId, Long date, String status);

    List<EmailOutreachCampaignMaturityRecordRegister> findByCampaignIdOrderByDateAsc(Long campaignId);

    List<EmailOutreachCampaignMaturityRecordRegister> findByStatus(String status);

}
