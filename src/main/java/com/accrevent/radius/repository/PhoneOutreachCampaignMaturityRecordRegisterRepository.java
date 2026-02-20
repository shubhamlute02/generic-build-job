package com.accrevent.radius.repository;

import com.accrevent.radius.model.LinkedInOutreachCampaignMaturityRecordRegister;
import com.accrevent.radius.model.PhoneOutreachCampaignMaturityRecordRegister;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhoneOutreachCampaignMaturityRecordRegisterRepository extends JpaRepository<PhoneOutreachCampaignMaturityRecordRegister,Long> {

    Optional<PhoneOutreachCampaignMaturityRecordRegister> findByCampaignIdAndDateAndStatus(
            Long campaignId, Long date, String status);

    List<PhoneOutreachCampaignMaturityRecordRegister> findByCampaignIdOrderByDateAsc(Long campaignId);

    List<PhoneOutreachCampaignMaturityRecordRegister> findByStatus(String status);


}
