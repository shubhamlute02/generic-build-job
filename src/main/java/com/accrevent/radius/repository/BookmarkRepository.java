package com.accrevent.radius.repository;

import com.accrevent.radius.model.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByUserNameAndLeadId(String userName, Long leadId);
    Optional<Bookmark> findByUserNameAndOpportunityId(String userName, Long opportunityId);
}

