package com.accrevent.radius.repository;

import com.accrevent.radius.model.Campaign;
import com.accrevent.radius.model.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;

public interface OpportunityRepository extends JpaRepository<Opportunity,Long> {

    List<Opportunity> findByWorkspace_WorkspaceId(Long workspaceId);

    @Query("SELECT DISTINCT o FROM Opportunity o " +
            "LEFT JOIN o.opportunitylifecycleList lc " +
            "LEFT JOIN lc.taskList t " +
            "WHERE t.assignTo = :assignTo " +
            "AND t.dueDate BETWEEN :startDate AND :endDate " )
    List<Opportunity> findOpportunityByTaskIdAndDueDateRange(@Param("assignTo") String taskId,
                                                       @Param("startDate") Long startDate,
                                                       @Param("endDate") Long endDate);


    List<Opportunity> findByBusinessUnit(String businessUnit);

    @Modifying
    @Query("UPDATE Opportunity o SET o.businessUnit = 'IOT' WHERE o.businessUnit = 'IoT'")
    int updateOpportunityBusinessUnit();

}
