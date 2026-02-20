package com.accrevent.radius.repository;

import com.accrevent.radius.Serializable.UserOpportunityCommentTimeTrack;
import com.accrevent.radius.model.OpportunityCommentTrack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpportunityCommentTrackRepository extends JpaRepository<OpportunityCommentTrack, UserOpportunityCommentTimeTrack>
{

}
