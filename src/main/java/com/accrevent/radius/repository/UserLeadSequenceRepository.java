package com.accrevent.radius.repository;

import com.accrevent.radius.model.UserLeadSequence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserLeadSequenceRepository extends JpaRepository<UserLeadSequence,Long> {

    List<UserLeadSequence> findByUserName(String username);

    List<UserLeadSequence> findByUserNameAndLead_LeadIdIn( String userName,List<Long> leadIds);
}
