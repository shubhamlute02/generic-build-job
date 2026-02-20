package com.accrevent.radius.repository;

import com.accrevent.radius.model.Lifecycle;
import com.accrevent.radius.dto.LeadLifecycleCountDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface LifecycleRepository extends JpaRepository<Lifecycle,Long> {
    List<Lifecycle> findByCampaign_CampaignId(Long campaignId);
    List<Lifecycle> findByCampaign_CampaignIdAndTypeIgnoreCase(Long campaignId, String type);
    List<Lifecycle> findByLead_LeadId(Long leadId);
    List<Lifecycle> findByOpportunity_OpportunityId(Long opportunityId);
    List<Lifecycle> findByVersion_versionId(Long versionId);

    @Transactional
    @Modifying
    @Query("UPDATE Lifecycle l SET l.status = :status WHERE l.lifecycleId = :lifecycleId")
    int updateLifeCycleStatus(@Param("lifecycleId")Long lifecycleId, @Param("status")String status);

    @Query("""
    SELECT new com.accrevent.radius.dto.LeadLifecycleCountDTO(
        COALESCE(SUM(CASE WHEN l.lifecycleName = 'identified' THEN 1 ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN l.lifecycleName = 'research' THEN 1 ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN l.lifecycleName = 'prospecting' THEN 1 ELSE 0 END), 0)
    )
    FROM Lifecycle l
    LEFT JOIN l.lead ld
    LEFT JOIN l.campaign c
    LEFT JOIN c.workspace w
    WHERE 
    (
        (:startDate IS NULL OR :endDate IS NULL)
        OR
        (
         (c.createdOn BETWEEN :startDate AND :endDate)
            OR 
         (ld.createdOn BETWEEN :startDate AND :endDate)
        )
    )
    AND l.status = 'active'
    AND (:workspaceId IS NULL OR w.workspaceId = :workspaceId)
    AND (:campaignId IS NULL OR c.campaignId = :campaignId)
""")
    LeadLifecycleCountDTO getLeadPipelineLifecycleCounts(
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate,
            @Param("workspaceId") Long workspaceId,
            @Param("campaignId") Long campaignId
    );

    List<Lifecycle> findByTypeIgnoreCase(String type);

    Optional<Lifecycle> findByType(String outreach);

//    @Query("SELECT l FROM Lifecycle l WHERE l.lead.campaign.campaignId = :campaignId AND l.type = :type")
//    List<Lifecycle> findByCampaignIdAndType(@Param("campaignId") Long campaignId, @Param("type") String type);

    // Find lifecycles linked to a Campaign
    List<Lifecycle> findByLifecycleNameAndCampaignIsNotNull(String lifecycleName);

    // Find lifecycles linked to a Lead
    List<Lifecycle> findByLifecycleNameAndLeadIsNotNull(String lifecycleName);

    // Find lifecycles linked to an Opportunity
    List<Lifecycle> findByLifecycleNameAndOpportunityIsNotNull(String lifecycleName);
}
