package com.accrevent.radius.repository;

import com.accrevent.radius.Serializable.UserCampaignCommentTimeTrack;
import com.accrevent.radius.model.CampaignCommentTrack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignCommentTrackRepository extends JpaRepository<CampaignCommentTrack, UserCampaignCommentTimeTrack>
{

}
