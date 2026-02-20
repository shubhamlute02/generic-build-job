package com.accrevent.radius.repository;

import com.accrevent.radius.model.Campaign;
import com.accrevent.radius.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead,Long> {

    @Query("SELECT l FROM Lead l JOIN FETCH l.campaign WHERE l.leadName = :leadName AND l.campaign.campaignId = :campaignId")
    Optional<Lead> findByLeadNameAndCampaignIdWithFetch(@Param("leadName") String leadName, @Param("campaignId") Long campaignId);



    List<Lead> findByCampaign_CampaignId(Long campaignId);
    @Query("SELECT l FROM Lead l LEFT JOIN FETCH l.leadlifecycleList WHERE l.leadName = :leadName AND l.campaign = :campaign")
    Optional<Lead> findByLeadNameAndCampaign(@Param("leadName") String leadName, @Param("campaign") Campaign campaign);

    List<Lead> findByBusinessUnit(String businessUnit);

    @Modifying
    @Query("UPDATE Lead l SET l.businessUnit = 'IOT' WHERE l.businessUnit = 'IoT'")
    int updateLeadBusinessUnit();

}
