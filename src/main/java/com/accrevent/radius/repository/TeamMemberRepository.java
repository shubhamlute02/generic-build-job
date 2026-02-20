package com.accrevent.radius.repository;

import com.accrevent.radius.model.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamMemberRepository extends JpaRepository<TeamMember,Long> {
    List<TeamMember> findByWorkspace_WorkspaceId(Long workspaceId);

    List<TeamMember> findByUserId(String userName);
    List<TeamMember> findByUserIdAndWorkspace_WorkspaceId(String userId, Long workspaceId);

    boolean existsByUserIdAndWorkspace_WorkspaceId(String userId, Long workspaceId);
}
