package com.accrevent.radius.repository;


import com.accrevent.radius.model.UserCampaignSpecSeq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCampaignSpecSeqRepository extends JpaRepository<UserCampaignSpecSeq,Long> {

    List<UserCampaignSpecSeq> findByUserName(String username);

    List<UserCampaignSpecSeq> findByUserNameAndCampaignSpecification_SpecificationIdIn( String userName,List<Long> specificationIds);
}
