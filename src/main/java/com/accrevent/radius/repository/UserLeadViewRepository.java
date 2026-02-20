package com.accrevent.radius.repository;

import com.accrevent.radius.model.UserLeadView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLeadViewRepository extends JpaRepository<UserLeadView, Long> {
    Optional<UserLeadView> findByUserNameAndLeadId(String userName, Long leadId);
}

