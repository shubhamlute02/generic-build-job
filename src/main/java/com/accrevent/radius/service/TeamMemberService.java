package com.accrevent.radius.service;


import com.accrevent.radius.dto.TeamMemberDTO;
import com.accrevent.radius.model.TeamMember;
import com.accrevent.radius.model.UserWorkspaceSequence;
import com.accrevent.radius.model.Workspace;
import com.accrevent.radius.repository.TeamMemberRepository;
import com.accrevent.radius.repository.UserWorkspaceSequenceRepository;
import com.accrevent.radius.repository.WorkspaceRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TeamMemberService {
    private final TeamMemberRepository teamMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserWorkspaceSequenceRepository userWorkspaceSequenceRepository;
    public TeamMemberService(TeamMemberRepository teamMemberRepository,
                             WorkspaceRepository workspaceRepository, UserWorkspaceSequenceRepository userWorkspaceSequenceRepository)
    {
        this.teamMemberRepository = teamMemberRepository;
        this.workspaceRepository = workspaceRepository;
        this.userWorkspaceSequenceRepository = userWorkspaceSequenceRepository;
    }

//    public TeamMemberDTO addTeamMember(TeamMemberDTO teamMemberDTO)
//    {
//        TeamMember teamMember = transformToTeam(teamMemberDTO);
//        return transformToTeamDTO(teamMemberRepository.save(teamMember));
//    }

    public TeamMemberDTO addTeamMember(TeamMemberDTO teamMemberDTO) {
        Optional<Workspace> workspaceOpt = workspaceRepository.findById(teamMemberDTO.getWorkspaceId());
        if (!workspaceOpt.isPresent()) {
            throw new IllegalArgumentException("Workspace ID not found");
        }
        Workspace workspace = workspaceOpt.get();

        // Save in team_member table
        TeamMember teamMember = new TeamMember();
        teamMember.setUserId(teamMemberDTO.getUserId());
        teamMember.setWorkspace(workspace);
        TeamMember savedTeamMember = teamMemberRepository.save(teamMember);

        // Also save in user_workspace_sequence table
        UserWorkspaceSequence uws = new UserWorkspaceSequence();
        uws.setUserName(teamMemberDTO.getUserId());
        uws.setWorkspace(workspace);
        uws.setSequenceOrder(999); // or some default sequence
        userWorkspaceSequenceRepository.save(uws);

        return transformToTeamDTO(savedTeamMember);
    }


    public Optional<TeamMember> getTeamById(Long id)
    {
        return teamMemberRepository.findById(id);
    }


    @Transactional
    public boolean deleteTeamMember(Long id) {
        System.out.println("id in service"+id);
        Optional<TeamMember> teamMemberOpt = teamMemberRepository.findById(id);
        if (teamMemberOpt.isPresent()) {
            TeamMember teamMember = teamMemberOpt.get();
            System.out.println("userId in team member: " + teamMember.getUserId());
            System.out.println("WorkspaceId in team member: " +
                    (teamMember.getWorkspace() != null ? teamMember.getWorkspace().getWorkspaceId() : "null"));
            // First delete the workspace sequence
            userWorkspaceSequenceRepository.deleteByUserNameAndWorkspace_WorkspaceId(
                    teamMember.getUserId(),   // or username field depending on your mapping
                    teamMember.getWorkspace().getWorkspaceId()


            );

            // Then delete the team member entry
//            teamMemberRepository.delete(teamMember);
            System.out.println("Checking exists before delete: " + teamMemberRepository.existsById(id));
            teamMemberRepository.deleteById(id);
            System.out.println("id deleted");
            return true;
        }
        return false;
    }




    public List<TeamMemberDTO> getTeamByWorkspaceId(Long workSpaceId)
    {
        Optional<Workspace>workspace = workspaceRepository.findById(workSpaceId);
        if(workspace.isPresent()) {
            List<TeamMemberDTO> teamMemberDTOList = new ArrayList<>();
            teamMemberRepository.findByWorkspace_WorkspaceId(workSpaceId).forEach(teamMember ->
                    teamMemberDTOList.add(transformToTeamDTO(teamMember))
            );
            return teamMemberDTOList;
        }else {
            throw new IllegalArgumentException("given Workspace ID does not exist");
        }
    }
    private TeamMember transformToTeam(TeamMemberDTO teamMemberDTO)
    {
        Optional<Workspace>workspace = workspaceRepository.findById(teamMemberDTO.getWorkspaceId());
        if(workspace.isPresent())
        {
            TeamMember teamMember = new TeamMember();
            if(teamMemberDTO.getTeamMemberId()!= null) {
                teamMember.setTeamMemberId(teamMember.getTeamMemberId());
            }
            teamMember.setUserId(teamMemberDTO.getUserId());
            teamMember.setWorkspace(workspace.get());
            return teamMember;
        }
        else {
            throw new IllegalArgumentException("Workspace ID not found");
        }
    }

    private TeamMemberDTO transformToTeamDTO(TeamMember teamMember)
    {
        TeamMemberDTO teamMemberDTO = new TeamMemberDTO();
        teamMemberDTO.setTeamMemberId(teamMember.getTeamMemberId());
        teamMemberDTO.setUserId(teamMember.getUserId());
        teamMemberDTO.setWorkspaceId(teamMember.getWorkspace().getWorkspaceId());
        return teamMemberDTO;
    }
    
    
}
