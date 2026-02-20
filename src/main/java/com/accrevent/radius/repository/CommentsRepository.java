package com.accrevent.radius.repository;

import com.accrevent.radius.model.Comments;
import com.accrevent.radius.model.Lifecycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentsRepository extends JpaRepository<Comments,Long> {
    List<Comments> findByCampaignCampaignIdOrderByCreatedOnDesc(Long campaignId);
    List<Comments> findByLeadLeadIdOrderByCreatedOnDesc(Long leadId);
    List<Comments> findByOpportunityOpportunityIdOrderByCreatedOnDesc(Long opportunityId);
    List<Comments> findByTaskTaskIdOrderByCreatedOnDesc(Long taskId);
    List<Comments> findByTaskLifecycleLeadLeadIdOrderByCreatedOnDesc(Long leadId);
    List<Comments> findByTaskTaskIdInOrderByCreatedOnDesc(List<Long> taskIds);
    List<Comments> findByVersion_VersionIdOrderByCreatedOnDesc(Long versionId);

    List<Comments> findByTask_TaskIdOrderByCreatedOnDesc(Long taskId);
}
