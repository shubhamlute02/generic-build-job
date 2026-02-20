package com.accrevent.radius.repository;

import com.accrevent.radius.model.UserWorkspaceSequence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserWorkspaceSequenceRepository extends JpaRepository<UserWorkspaceSequence,Long> {

    List<UserWorkspaceSequence> findByUserName(String username);

    void deleteByUserNameAndWorkspace_WorkspaceId(String userName, Long workspaceId);
    List<UserWorkspaceSequence> findByUserNameAndWorkspace_WorkspaceIdIn( String userName,List<Long> workspaceIds);
}
