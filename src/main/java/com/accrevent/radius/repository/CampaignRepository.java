package com.accrevent.radius.repository;

import com.accrevent.radius.model.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign,Long> {

    List<Campaign> findByWorkspace_WorkspaceId(Long workspaceId);
    List<Campaign> findByType(String type);
    List<Campaign> findByWorkspace_WorkspaceIdAndType(Long workspaceId, String type);



    @Query("SELECT DISTINCT c FROM Campaign c " +
            "LEFT JOIN c.lifecycleList lc " +
            "LEFT JOIN lc.taskList t " +
            "LEFT JOIN c.leadList l " +
            "LEFT JOIN l.leadlifecycleList ll " +
            "LEFT JOIN ll.taskList lt " +
            "WHERE (t.assignTo = :assignTo OR lt.assignTo = :assignTo) " +
            "AND ((t.dueDate BETWEEN :startDate AND :endDate) " +
            "OR (lt.dueDate BETWEEN :startDate AND :endDate))")
    List<Campaign> findCampaignByTaskIdAndDueDateRange(@Param("assignTo") String taskId,
                                            @Param("startDate") Long startDate,
                                            @Param("endDate") Long endDate);
}
