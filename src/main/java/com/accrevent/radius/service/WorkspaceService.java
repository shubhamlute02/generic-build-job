package com.accrevent.radius.service;

import com.accrevent.radius.controller.WorkspaceController;
import com.accrevent.radius.dto.CampaignDTO;
import com.accrevent.radius.dto.OpportunityDTO;

import com.accrevent.radius.exception.ResourceNotFoundException;
import com.accrevent.radius.model.*;
import com.accrevent.radius.repository.*;
import com.accrevent.radius.dto.WorkspaceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final UserCampaignSequenceRepository userCampaignSequenceRepository;
    private final UserOpportunitySequenceRepository userOpportunitySequenceRepository;
    private final UserWorkspaceSequenceRepository userWorkspaceSequenceRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CampaignService campaignService;
    private final TeamMemberService teamMemberService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceService.class);


    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            UserCampaignSequenceRepository userCampaignSequenceRepository, UserOpportunitySequenceRepository userOpportunitySequenceRepository,
                            UserWorkspaceSequenceRepository userWorkspaceSequenceRepository, TeamMemberRepository teamMemberRepository, CampaignService campaignService, TeamMemberService teamMemberService)
    {
        this.workspaceRepository = workspaceRepository;
        this.userCampaignSequenceRepository = userCampaignSequenceRepository;
        this.userOpportunitySequenceRepository = userOpportunitySequenceRepository;
        this.userWorkspaceSequenceRepository = userWorkspaceSequenceRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.campaignService = campaignService;
        this.teamMemberService = teamMemberService;
    }

    public WorkspaceDTO createWorkspace(WorkspaceDTO workspaceDTO)
    {
        Workspace workspace = transformToWorkspace(workspaceDTO);
        if(workspace.getWorkspaceId() == null) {
            if (workspaceRepository.existsByWorkspaceName(workspace.getWorkspaceName())) {
                throw new IllegalArgumentException("Workspace with the same name already exists.");
            }
        }
        // Save the workspace first
        Workspace savedWorkspace = workspaceRepository.save(workspace);

        // Create and save team member for the owner
        TeamMember ownerMember = new TeamMember();
        ownerMember.setUserId(savedWorkspace.getOwner()); // Set owner's user ID
        ownerMember.setWorkspace(savedWorkspace);        // Associate with the workspace
        teamMemberRepository.save(ownerMember);

        return transformToWorkspaceDTO(savedWorkspace, "");

    }

    public List<WorkspaceDTO> getAllWorkspace(String userName) {
        List<WorkspaceDTO> workspaceDTOList = new ArrayList<>();

        //  Fetch all workspaces
        List<Workspace> allWorkspaces = workspaceRepository.findAll();


        //  Find workspaces where user is a team member (team_member table)
        List<TeamMember> teamMemberships = teamMemberRepository.findByUserId(userName);
        Set<Long> memberWorkspaceIds = teamMemberships.stream()
                .map(tm -> tm.getWorkspace().getWorkspaceId())
                .collect(Collectors.toSet());

        //  Also fetch user-specific sequence preferences
        List<UserWorkspaceSequence> userWorkspaceSequences =
                userWorkspaceSequenceRepository.findByUserName(userName);

        logger.info("User {} has {} workspace sequence preferences", userName, userWorkspaceSequences.size());


        // Build a map: workspaceId -> sequenceOrder (if user has preferences)
        Map<Long, Integer> userSpecificSequences = userWorkspaceSequences.stream()
                .collect(Collectors.toMap(
                        seq -> seq.getWorkspace().getWorkspaceId(),
                        UserWorkspaceSequence::getSequenceOrder
                ));

        logger.info("User specific sequences for {} => {}", userName, userSpecificSequences);



        // User can see workspaces where:
        // - they are the owner
        // - they are a team member
        List<Workspace> userWorkspaces = allWorkspaces.stream()
                .filter(workspace ->
                        userName.equals(workspace.getOwner()) ||
                                memberWorkspaceIds.contains(workspace.getWorkspaceId())
                )
                .collect(Collectors.toList());

        // Sort workspaces based on user's sequenceOrder, if available
        List<Workspace> finalWorkspaceList = userWorkspaces.stream()
                .sorted((w1, w2) -> {
                    int seq1 = userSpecificSequences.getOrDefault(w1.getWorkspaceId(), getDefaultSequenceOrder());
                    int seq2 = userSpecificSequences.getOrDefault(w2.getWorkspaceId(), getDefaultSequenceOrder());
                    return Integer.compare(seq1, seq2);
                })
                .collect(Collectors.toList());

        // 6️⃣ Convert to DTOs
        finalWorkspaceList.forEach(workspace ->
                workspaceDTOList.add(transformToWorkspaceDTO(workspace, userName))
        );

        logger.info("getAllWorkspace returning {} workspaces for user={}", workspaceDTOList.size(), userName);
        logger.info("Final WorkspaceDTO list for {} => {}", userName, workspaceDTOList);

        return workspaceDTOList;

    }





    public Optional<Workspace> getWorkspaceById(Long id)
    {
        return workspaceRepository.findById(id);
    }

    public boolean deleteWorkspace(Long id)
    {
        if(workspaceRepository.existsById(id))
        {
            workspaceRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private Workspace transformToWorkspace(WorkspaceDTO workspaceDTO)
    {
        Workspace workspace = new Workspace();
        if(workspaceDTO.getWorkspaceId()!= null) {
            workspace.setWorkspaceId(workspaceDTO.getWorkspaceId());
        }
        workspace.setWorkspaceName(workspaceDTO.getWorkspaceName());
        workspace.setDescription(workspaceDTO.getDescription());
        workspace.setOwner(workspaceDTO.getOwner());
        if(workspaceDTO.getCreatedOn() == null)
        {
            workspace.setCreatedOn(ZonedDateTime.now());
        }
        return workspace;
    }



    private WorkspaceDTO transformToWorkspaceDTO(Workspace workspace, String userName) {
        WorkspaceDTO workspaceDTO = new WorkspaceDTO();
        workspaceDTO.setWorkspaceId(workspace.getWorkspaceId());
        workspaceDTO.setWorkspaceName(workspace.getWorkspaceName());
        workspaceDTO.setDescription(workspace.getDescription());
        workspaceDTO.setOwner(workspace.getOwner());

        if (workspace.getCreatedOn() != null) {
            workspaceDTO.setCreatedOn(workspace.getCreatedOn().format(formatter));
        }

        // Get user sequences for campaigns and opportunities
        List<UserCampaignSequence> userCampaignSequences = userCampaignSequenceRepository.findByUserName(userName);
        Map<Long, Integer> campaignSequences = userCampaignSequences.stream()
                .collect(Collectors.toMap(
                        seq -> seq.getCampaign().getCampaignId(),
                        UserCampaignSequence::getSequenceOrder
                ));

        // Process all campaigns (regular + outreach)
        List<Campaign> allCampaigns = workspace.getCampaigns();

        // Separate regular and outreach campaigns
        Map<Boolean, List<Campaign>> partitionedCampaigns = allCampaigns.stream()
                .collect(Collectors.partitioningBy(
                        c -> c.getType() != null && c.getType().equalsIgnoreCase("OUTREACH")
                ));

        List<Campaign> regularCampaigns = partitionedCampaigns.get(false);
        List<Campaign> outreachCampaigns = partitionedCampaigns.get(true);

        // Transform and sort regular campaigns
        List<CampaignDTO> regularCampaignDTOs = transformAndSortCampaigns(regularCampaigns, campaignSequences);
        workspaceDTO.setCampaigns(regularCampaignDTOs);

        // Transform and sort outreach campaigns
        List<CampaignDTO> outreachCampaignDTOs = transformAndSortCampaigns(outreachCampaigns, campaignSequences);
        workspaceDTO.setOutreachCampaigns(outreachCampaignDTOs);

        // Process opportunities
        List<Opportunity> allOpportunities = workspace.getOpportunityList();
        List<UserOpportunitySequence> userOpportunitySequences = userOpportunitySequenceRepository.findByUserName(userName);

        Map<Long, Integer> opportunitySequences = userOpportunitySequences.stream()
                .collect(Collectors.toMap(
                        seq -> seq.getOpportunity().getOpportunityId(),
                        UserOpportunitySequence::getSequenceOrder
                ));

        List<Opportunity> sortedOpportunities = allOpportunities.stream()
                .sorted((o1, o2) -> {
                    int seq1 = opportunitySequences.getOrDefault(o1.getOpportunityId(), getDefaultSequenceOrder());
                    int seq2 = opportunitySequences.getOrDefault(o2.getOpportunityId(), getDefaultSequenceOrder());
                    return Integer.compare(seq1, seq2);
                })
                .collect(Collectors.toList());

        List<OpportunityDTO> opportunityDTOs = sortedOpportunities.stream()
                .map(this::transformToOpportunityDTO)
                .collect(Collectors.toList());

        workspaceDTO.setOpportunities(opportunityDTOs);

        // Count valid opportunities
        long validOpportunityCount = sortedOpportunities.stream()
                .filter(this::isOpportunityValid)
                .count();
        workspaceDTO.setOpportunityCount(validOpportunityCount);

        return workspaceDTO;
    }

    // Helper method to transform and sort campaigns
    private List<CampaignDTO> transformAndSortCampaigns(List<Campaign> campaigns, Map<Long, Integer> sequenceMap) {
        return campaigns.stream()
                .sorted((c1, c2) -> {
                    int seq1 = sequenceMap.getOrDefault(c1.getCampaignId(), getDefaultSequenceOrder());
                    int seq2 = sequenceMap.getOrDefault(c2.getCampaignId(), getDefaultSequenceOrder());
                    return Integer.compare(seq1, seq2);
                })
                .map(this::transformToCampaignDTO)
                .collect(Collectors.toList());
    }

    // Helper method to transform campaign to DTO
    private CampaignDTO transformToCampaignDTO(Campaign campaign) {
        CampaignDTO dto = new CampaignDTO();
        dto.setCampaignId(campaign.getCampaignId());
        dto.setCampaignName(campaign.getCampaignName());
        dto.setDescription(campaign.getDescription());
        dto.setWorkspaceId(campaign.getWorkspace().getWorkspaceId());
        dto.setOwner(campaign.getOwner());
        dto.setCreatedOn(campaign.getCreatedOn());
        dto.setType(campaign.getType());
        dto.setIsSystemComment(campaign.getIsSystemComment());
        dto.setActualEndDate(campaign.getActualEndDate());
        dto.setActualStartDate(campaign.getActualStartDate());
        dto.setPlannedEndDate(campaign.getPlannedEndDate());
        dto.setPlannedStartDate(campaign.getPlannedStartDate());
        return dto;
    }

    // Helper method to transform opportunity to DTO
    private OpportunityDTO transformToOpportunityDTO(Opportunity opportunity) {
        OpportunityDTO dto = new OpportunityDTO();
        dto.setOpportunityId(opportunity.getOpportunityId());
        dto.setOpportunityName(opportunity.getOpportunityName());
        dto.setDescription(opportunity.getDescription());
        dto.setWorkspaceId(opportunity.getWorkspace().getWorkspaceId());
        dto.setRequirement(opportunity.getRequirement());
        dto.setCustomer(opportunity.getCustomer());
        dto.setEstimateRevenue(opportunity.getEstimateRevenue());
        dto.setCurrency(opportunity.getCurrency());
        dto.setProjectTitle(opportunity.getProjectTitle());
        dto.setBusinessUnit(opportunity.getBusinessUnit());
        return dto;
    }

    // Helper method to check if opportunity is valid
    private boolean isOpportunityValid(Opportunity opportunity) {
        List<Lifecycle> lifecycles = opportunity.getOpportunitylifecycleList();
        Optional<Lifecycle> current = lifecycles.stream()
                .filter(lc -> lc.getStatus() != null && lc.getStatus().equalsIgnoreCase("active"))
                .findFirst();
        return current.isPresent() &&
                !(current.get().getLifecycleName().equalsIgnoreCase("closed won") ||
                        current.get().getLifecycleName().equalsIgnoreCase("closed lost"));
    }



    public int getDefaultSequenceOrder() {
        return 999; // Example of default sequence order, change as per logic
    }
    public List<UserWorkspaceSequence> updateWorkspaceSequence(String userName, List<WorkspaceDTO> orderedWorkspaceDTOList)
    {
        logger.info("Input orderedWorkspaceDTOList for {} => {}", userName, orderedWorkspaceDTOList);

        int workspaceLength = orderedWorkspaceDTOList.size();
        System.out.println("workspaceLength" + workspaceLength);
        List<UserWorkspaceSequence> sequencesToSave = new ArrayList<>();

        List<Long> workspaceIds = orderedWorkspaceDTOList.stream()
                .map(WorkspaceDTO::getWorkspaceId)
                .collect(Collectors.toList());

        for (Long id : workspaceIds) {
            System.out.println("Workspace ID*************: " + id);
        }


        List<UserWorkspaceSequence> existingSequences = userWorkspaceSequenceRepository.findByUserNameAndWorkspace_WorkspaceIdIn(userName, workspaceIds);
        System.out.println("Existing Sequences:");
        for (UserWorkspaceSequence seq : existingSequences) {
            System.out.println(
                    "ID: " + seq.getUserWorkspaceSequenceId() +
                            ", Workspace ID: " + (seq.getWorkspace() != null ? seq.getWorkspace().getWorkspaceId() : null) +
                            ", Sequence Order: " + seq.getSequenceOrder() +
                            ", User: " + seq.getUserName()
            );
        }


//        Converts the List into a Map(key= workspace id, value= UserWorkspaceSequence object)
        Map<Long, UserWorkspaceSequence> sequenceMap = existingSequences.stream()
                .collect(Collectors.toMap(seq -> seq.getWorkspace().getWorkspaceId(), seq -> seq));

        for(int i = 0; i < workspaceLength ; i++)
        {
            Long workspaceId = orderedWorkspaceDTOList.get(i).getWorkspaceId();
            int newSequenceOrder = i + 1;

            UserWorkspaceSequence userWorkspaceSequence = sequenceMap.get(workspaceId);

            if (userWorkspaceSequence != null) {
                // Update the sequence order if needed
                if (userWorkspaceSequence.getSequenceOrder() != newSequenceOrder) {
                    System.out.println("userWorkspaceSequence.getSequenceOrder" + userWorkspaceSequence.getSequenceOrder());
                    System.out.println("newSequenceOrder" + newSequenceOrder);
                    logger.info("Updating sequence for workspaceId={} from {} to {}",
                            workspaceId, userWorkspaceSequence.getSequenceOrder(), newSequenceOrder);

                    System.out.println("cccc entry# " + (i + 1));

                    userWorkspaceSequence.setSequenceOrder(newSequenceOrder);
                    sequencesToSave.add(userWorkspaceSequence);
                }
            }
//            So number of duplicates created = number of input workspaceIds missing from map
            else {
                // If no sequence exists, create a new one
                Optional<Workspace> workspace = workspaceRepository.findById(workspaceId);
                if (workspace.isPresent()) {
                    UserWorkspaceSequence newWorkspaceSequence = new UserWorkspaceSequence();
                    newWorkspaceSequence.setWorkspace(workspace.get());
                    newWorkspaceSequence.setSequenceOrder(newSequenceOrder);
                    newWorkspaceSequence.setUserName(userName);
                    logger.info("Creating new sequence for workspaceId={} with order={}",
                            workspaceId, newSequenceOrder);
                    sequencesToSave.add(newWorkspaceSequence);
                    System.out.println("sequencesToSave in else block" + sequencesToSave);
                }
            }
        }
        if (!sequencesToSave.isEmpty()) {

            userWorkspaceSequenceRepository.saveAll(sequencesToSave);
        }
        logger.info("Final sequencesToSave for {} => {}",
                userName,
                sequencesToSave.stream()
                        .map(seq -> String.format(
                                "{sequenceId=%d, workspaceId=%d, order=%d}",
                                seq.getUserWorkspaceSequenceId(),
                                seq.getWorkspace() != null ? seq.getWorkspace().getWorkspaceId() : null,
                                seq.getSequenceOrder()
                        ))
                        .toList()
        );

        return sequencesToSave;
    }

    public String updateNameAndDescriptionAndOwnerByWorkspaceId(Long workspaceId,String workspaceName, String description,String owner){
        try{
            Optional<Workspace> workspaceOptional = workspaceRepository.findById(workspaceId);
            if(workspaceOptional.isPresent()){
                Workspace workspace = workspaceOptional.get();
                String previousOwner = workspace.getOwner();

                workspace.setWorkspaceName(workspaceName);
                workspace.setDescription(description);
//

                // Update workspace details
                workspace.setWorkspaceName(workspaceName);
                workspace.setDescription(description);

                // Only process team member changes if owner is actually changing
                if (!owner.equals(previousOwner)) {
                    // 1. Find and remove previous owner's default membership
                    List<TeamMember> previousOwnerMembers = teamMemberRepository
                            .findByUserIdAndWorkspace_WorkspaceId(previousOwner, workspaceId);

                    // Use your existing delete method for each matching member
                    for (TeamMember member : previousOwnerMembers) {
                        teamMemberService.deleteTeamMember(member.getTeamMemberId());
                    }

                    // 2. Add new owner as member if not already present
                    boolean newOwnerAlreadyMember = teamMemberRepository
                            .existsByUserIdAndWorkspace_WorkspaceId(owner, workspaceId);
                    if (!newOwnerAlreadyMember) {
                        TeamMember newOwnerMember = new TeamMember();
                        newOwnerMember.setUserId(owner);
                        newOwnerMember.setWorkspace(workspace);
                        teamMemberRepository.save(newOwnerMember);
                    }
                }
                workspace.setOwner(owner);
                workspaceRepository.save(workspace);
                return "Workspace updated Successfully.";
            }else{
                throw new ResourceNotFoundException("Workspace not Found.");
            }
        }catch (Exception e)
        {
            return "Error = "+e.getMessage();
        }
    }
}
