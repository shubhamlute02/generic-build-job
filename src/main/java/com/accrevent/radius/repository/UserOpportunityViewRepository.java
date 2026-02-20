
package com.accrevent.radius.repository;

import com.accrevent.radius.model.UserOpportunityView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserOpportunityViewRepository extends JpaRepository<UserOpportunityView, Long> {
    Optional<UserOpportunityView> findByUserNameAndOpportunityId(String userName, Long opportunityId);
}

