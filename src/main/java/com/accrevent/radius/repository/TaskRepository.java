package com.accrevent.radius.repository;
import com.accrevent.radius.dto.TaskWeeklyPlannerDTO;
import com.accrevent.radius.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task,Long>
{
    List<Task> findByLifecycle_LifecycleId(Long lifecycleId);
    @Transactional
    @Modifying
    @Query("UPDATE Task t SET t.sendToAssignee = :sendToAssignee WHERE t.taskId = :taskId")
    int updateSendToAssignee(@Param("taskId")Long taskId, @Param("sendToAssignee")boolean sendToAssignee);

    List<Task> findByStatus(String status);


    List<EmailOutreachTask> findByLifecycle_Campaign_CampaignIdAndStatusIgnoreCase(
            Long campaignId,
            String status
    );

    List<Task> findByStatusIgnoreCase(String status);


    @Transactional
    @Modifying
    @Query("UPDATE Task t SET t.status = :status WHERE t.taskId = :taskId")
    int updateTaskStatus(@Param("taskId")Long taskId, @Param("status")String status);

    @Query("SELECT t.taskId FROM Task t WHERE t.lifecycle.lead.leadId = :leadId")
    List<Long> findTaskIdsByLeadId(@Param("leadId") Long leadId);

    int countByLifecycle_Lead_Campaign_CampaignIdAndType(Long campaignId, String type);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.lifecycle l " +
            "LEFT JOIN FETCH l.lead ld " +
            "LEFT JOIN FETCH ld.campaign c " +
            "WHERE c.campaignId = :campaignId AND t.type = :type")
    List<Task> findByCampaignIdAndTypeWithLifecycle(@Param("campaignId") Long campaignId,
                                                    @Param("type") String type);



    @Query("""
    SELECT t 
    FROM Task t
    JOIN FETCH t.lifecycle l
    JOIN l.lead le
    JOIN le.campaign c
    WHERE c.campaignId = :campaignId
""")
    List<Task> findByCampaignIdWithLifecycle(@Param("campaignId") Long campaignId);


    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.subTasks WHERE t.lifecycle.lifecycleId = :lifecycleId")
        List<Task> findByLifecycleIdWithSubtasks(@Param("lifecycleId") Long lifecycleId);


    @Transactional
    @Modifying
    @Query("UPDATE Task t SET t.weeklyTaskSequence = :weeklyTaskSequence , t.dueDate = :dueDate WHERE t.taskId = :taskId")
    int updateDueDateAndWeeklyTaskSequence(@Param("taskId")Long taskId, @Param("dueDate")Long dueDate,@Param("weeklyTaskSequence")int weeklyTaskSequence);



    @Query("""
    SELECT new com.accrevent.radius.dto.TaskWeeklyPlannerDTO(
        t.taskId,
        t.taskName,
        t.description,
        COALESCE(l.lifecycleId, pl.lifecycleId),
        t.parentTask.taskId,
        t.assignTo,
        t.sendToAssignee,
        t.duration,
        t.durationValue,
        CAST(t.dueDate AS long),
        t.status,
        CAST(t.createdOn AS long),
        t.weeklyTaskSequence,
        CONCAT(
            COALESCE(w1.workspaceName, w2.workspaceName, w3.workspaceName, pw1.workspaceName, pw2.workspaceName, pw3.workspaceName, 'Unknown'), '',
            CASE WHEN c.campaignId IS NOT NULL OR lc.campaignId IS NOT NULL OR pc.campaignId IS NOT NULL OR plc.campaignId IS NOT NULL THEN 
                ' / ' || COALESCE(c.campaignName, lc.campaignName, pc.campaignName, plc.campaignName, '') 
            ELSE '' END,
            CASE WHEN le.leadId IS NOT NULL OR ple.leadId IS NOT NULL THEN 
                ' / ' || COALESCE(le.leadName, ple.leadName, '') 
            ELSE '' END,
            CASE WHEN o.opportunityName IS NOT NULL OR po.opportunityName IS NOT NULL THEN 
                ' / ' || COALESCE(o.opportunityName, po.opportunityName, '') 
            ELSE '' END,
            ' / ' || COALESCE(l.lifecycleName, pl.lifecycleName, ''),
            CASE WHEN t.parentTask.taskId IS NOT NULL THEN ' / ' || COALESCE(p.taskName, 'Unknown') ELSE '' END
        ),
        COALESCE(w1.workspaceId, w2.workspaceId, w3.workspaceId, pw1.workspaceId, pw2.workspaceId, pw3.workspaceId, 0L)
    )
    FROM Task t
    LEFT JOIN t.lifecycle l
    LEFT JOIN l.lead le
    LEFT JOIN le.campaign c
    LEFT JOIN l.campaign lc
    LEFT JOIN l.opportunity o
    LEFT JOIN c.workspace w1
    LEFT JOIN o.workspace w2
    LEFT JOIN lc.workspace w3

    LEFT JOIN t.parentTask p
    LEFT JOIN p.lifecycle pl
    LEFT JOIN pl.lead ple
    LEFT JOIN ple.campaign pc
    LEFT JOIN pl.campaign plc
    LEFT JOIN pl.opportunity po
    LEFT JOIN pc.workspace pw1
    LEFT JOIN po.workspace pw2
    LEFT JOIN plc.workspace pw3

    WHERE t.assignTo = :assignTo
      AND t.sendToAssignee = :sendToAssignee
      AND t.dueDate BETWEEN :startDate AND :endDate
      AND (l.lifecycleId IS NOT NULL OR p.taskId IS NOT NULL)
    ORDER BY t.weeklyTaskSequence ASC
""")
    List<TaskWeeklyPlannerDTO> getTasksByAssignToAndSendToAssigneeAndDueDateRange(
            @Param("assignTo") String assignTo,
            @Param("sendToAssignee") boolean sendToAssignee,
            @Param("startDate") Long startDate,
            @Param("endDate") Long endDate
    );



    @Transactional
    @Modifying
    @Query("UPDATE Task t SET t.assignTo = :assignTo , t.weeklyTaskSequence = :weeklyTaskSequence WHERE t.taskId = :taskId")
    int updateAssignToAndWeeklyTaskSequence(@Param("taskId")Long taskId, @Param("assignTo")String assignTo,@Param("weeklyTaskSequence")int weeklyTaskSequence);

    @Query("SELECT t FROM Task t WHERE t.assignTo IN :assignToList AND t.status IN :statusList AND t.dueDate <= :dueDate")
    List<Task> findTasksByAssignToListAndDueDate(
            @Param("assignToList") List<String> assignToList,
            @Param("statusList") List<String> statusList,
            @Param("dueDate") Long dueDate);

    @Query("SELECT t FROM EmailOutreachTask t WHERE t.relatedContact.contactId = :contactId AND t.lifecycle.lifecycleId = :lifecycleId")
    Optional<EmailOutreachTask> findByContactIdAndLifecycleId(@Param("contactId") Long contactId, @Param("lifecycleId") Long lifecycleId);

    @Query("SELECT t FROM EmailOutreachTask t WHERE t.relatedContact.contactId = :contactId AND t.lifecycle.lifecycleId = :lifecycleId")
    Optional<EmailOutreachTask> findEmailOutreachTask(@Param("contactId") Long contactId, @Param("lifecycleId") Long lifecycleId);

    @Query("SELECT t FROM PhoneOutreachTask t WHERE t.relatedContact.contactId = :contactId AND t.lifecycle.lifecycleId = :lifecycleId")
    Optional<PhoneOutreachTask> findPhoneOutreachTask(@Param("contactId") Long contactId, @Param("lifecycleId") Long lifecycleId);

    @Query("SELECT t FROM LinkedInOutreachTask t WHERE t.relatedContact.contactId = :contactId AND t.lifecycle.lifecycleId = :lifecycleId")
    Optional<LinkedInOutreachTask> findLinkedinOutreachTask(@Param("contactId") Long contactId, @Param("lifecycleId") Long lifecycleId);

    @Modifying
    @Query(value = "UPDATE task SET task_type = :newType WHERE task_type = 'outreach' OR task_type = 'Outreach' OR task_type = 'OUTREACH'", nativeQuery = true)
    int updateOutreachTaskType(@Param("newType") String newType);


    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Task t WHERE t.parentTask = :parentTask")
    boolean existsByParentTask(@Param("parentTask") Task parentTask);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.lifecycle.lifecycleId = :lifecycleId")
    int countByLifecycleLifecycleId(Long lifecycleId);

    List<Task> findByLifecycle_Lead_LeadId(Long leadId);

}
