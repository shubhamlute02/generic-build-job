package com.accrevent.radius.repository;


import com.accrevent.radius.model.UserCampaignSequence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCampaignSequenceRepository extends JpaRepository<UserCampaignSequence,Long> {

    List<UserCampaignSequence> findByUserName(String username);

    List<UserCampaignSequence> findByUserNameAndCampaign_CampaignIdIn( String userName,List<Long> campaignIds);
}
