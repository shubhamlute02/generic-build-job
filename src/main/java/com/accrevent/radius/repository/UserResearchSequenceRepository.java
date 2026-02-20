package com.accrevent.radius.repository;

import com.accrevent.radius.model.UserResearchSequence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface UserResearchSequenceRepository extends JpaRepository<UserResearchSequence, Long> {

    List<UserResearchSequence> findByUserNameIn(Set<String> userNames);

}
