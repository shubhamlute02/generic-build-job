package com.accrevent.radius.repository;

import com.accrevent.radius.model.PhoneCallingRegister;
import com.accrevent.radius.model.PhoneOutreachCampaignMaturityRecordRegister;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhoneCallingRegisterRepository extends JpaRepository<PhoneCallingRegister,Long> {

    Optional<PhoneCallingRegister> findByCampaignIdAndDateAndStatus(
            Long campaignId, Long date, String status);

    List<PhoneCallingRegister> findByCampaignIdOrderByDateAsc(Long campaignId);

}
