package com.accrevent.radius.repository;

import com.accrevent.radius.model.UserOpportunitySequence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserOpportunitySequenceRepository extends JpaRepository<UserOpportunitySequence,Long> {

    List<UserOpportunitySequence> findByUserName(String userName);
    List<UserOpportunitySequence> findByUserNameAndOpportunity_OpportunityIdIn( String userName,List<Long> opportunityIds);
}
