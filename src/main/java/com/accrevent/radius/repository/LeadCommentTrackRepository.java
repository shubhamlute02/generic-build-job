package com.accrevent.radius.repository;

import com.accrevent.radius.Serializable.UserLeadCommentTimeTrack;
import com.accrevent.radius.model.LeadCommentTrack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadCommentTrackRepository extends JpaRepository<LeadCommentTrack, UserLeadCommentTimeTrack>
{

}
