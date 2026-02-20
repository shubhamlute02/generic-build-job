package com.accrevent.radius.repository;

import com.accrevent.radius.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace,Long> {

    boolean existsByWorkspaceName(String workspaceName);

    @Transactional
    @Modifying
    @Query("UPDATE Workspace w SET w.owner = :owner WHERE w.workspaceId = :workspaceId")
    int updateWorkspaceOwner(@Param("workspaceId")Long workspaceId, @Param("owner")String owner);

    List<Workspace> findByOwner(String owner);



}
