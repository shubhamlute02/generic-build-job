package com.accrevent.radius.repository;

import com.accrevent.radius.model.MarketingMeetingRegister;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketingMeetingRegisterRepository extends JpaRepository<MarketingMeetingRegister, Long> {

    List<MarketingMeetingRegister> findByCampaign_CampaignId(Long campaignId);
}
