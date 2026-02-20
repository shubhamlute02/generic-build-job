package com.accrevent.radius.repository;
import com.accrevent.radius.model.CampaignSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CampaignSpecificationRepository extends JpaRepository<CampaignSpecification,Long> {

    List<CampaignSpecification> findByCampaign_CampaignId(Long campaignId);
}
