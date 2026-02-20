package com.accrevent.radius.repository;

import com.accrevent.radius.model.MarketingStory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketingStoryRepository extends JpaRepository <MarketingStory,Long> {

    List<MarketingStory> findByCollection_MarketingCollectionId(Long marketingCollectionId);
}
