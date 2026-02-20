package com.accrevent.radius.repository;


import com.accrevent.radius.model.UserTaskSequence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTaskSequenceRepository extends JpaRepository<UserTaskSequence,Long> {

    List<UserTaskSequence> findByUserName(String username);

    List<UserTaskSequence> findByUserNameAndTask_TaskIdIn( String userName,List<Long> taskIds);
}
