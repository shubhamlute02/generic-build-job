package com.accrevent.radius.service;


import com.accrevent.radius.model.*;
import com.accrevent.radius.repository.*;
import com.accrevent.radius.dto.LeadDTO;
import com.accrevent.radius.util.CampaignType;
import com.accrevent.radius.util.RadiusConstants;
import com.accrevent.radius.util.RadiusUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class LeadService {
    private static final Logger logger = LoggerFactory.getLogger(LeadService.class);

    private final LeadRepository leadRepository;
    private final CampaignRepository campaignRepository;
    private final ConstantLifecycleRepository constantLifecycleRepository;
    private final UserLeadSequenceRepository userLeadSequenceRepository;
    private final CommentsRepository commentsRepository;
    private final CompanyRepository companyRepository;
    @Autowired
    private UserLeadViewRepository userLeadViewRepository;
    public LeadService(LeadRepository leadRepository,
                       CampaignRepository campaignRepository,
                       ConstantLifecycleRepository constantLifecycleRepository,
                       UserLeadSequenceRepository userLeadSequenceRepository,
                       CommentsRepository commentsRepository, CompanyRepository companyRepository)
    {
        this.leadRepository = leadRepository;
        this.campaignRepository = campaignRepository;
        this.constantLifecycleRepository = constantLifecycleRepository;
        this.userLeadSequenceRepository = userLeadSequenceRepository;
        this.commentsRepository = commentsRepository;
        this.companyRepository = companyRepository;
    }


public LeadDTO createLead(LeadDTO leadDTO)
{
    Lead lead = transformToLead(leadDTO);

    if(lead.getLeadlifecycleList().isEmpty()) {
        List<Lifecycle> lifecycleList = new ArrayList<>();
        AtomicBoolean flag = new AtomicBoolean(true);
        List<ConstantLifecycle> constantLifecycles = constantLifecycleRepository.findByCycleId(2L);

        //new for setting type
        // Fetch campaign type if campaignId exists in LeadDTO
        String campaignType;
        if (leadDTO.getCampaignId() != null) {
            campaignType = campaignRepository.findById(leadDTO.getCampaignId())
                    .map(Campaign::getType) // Use the campaign's type
                    .orElse(CampaignType.DEFAULT); // Fallback to default if campaign not found
        } else {
            campaignType = CampaignType.DEFAULT;
        }

        //end for setting type of lifecycle for outreach task in lead

        constantLifecycles.forEach(constantLifecycle -> {
            Lifecycle lifecycle = new Lifecycle();
            lifecycle.setLifecycleName(constantLifecycle.getCycleName());
            lifecycle.setStatus("inActive");
            lifecycle.setType(campaignType);
            if(flag.get())
            {
                lifecycle.setStatus("active");
                flag.set(false);
            }
            lifecycle.setLead(lead);
            lifecycleList.add(lifecycle);
        });
        lead.setLeadlifecycleList(lifecycleList);

        // Save Lead first so it has ID for comment linkage
        Lead savedLead = leadRepository.save(lead);

        //new for red .rem
        // Use a single captured timestamp
        long createdOnTime = System.currentTimeMillis();

        // System-generated comment for lead creation
        String username = RadiusUtil.getCurrentUsername();

        StringBuilder commentBuilder = new StringBuilder();
        commentBuilder.append(username)
                .append(" created the lead.");

        Comments comment = new Comments();
        comment.setLead(savedLead);
        comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
        comment.setCommentDescription(commentBuilder.toString());
        comment.setCreatedOn(createdOnTime);
        comment.setIsSystemComment(true);

        commentsRepository.save(comment);

        // Update UserLeadView.lastLeadViewed using the SAME timestamp
        UserLeadView userLeadView = userLeadViewRepository
                .findByUserNameAndLeadId(username, savedLead.getLeadId())
                .orElseGet(() -> {
                    UserLeadView newView = new UserLeadView();
                    newView.setUserName(username);
                    newView.setLeadId(savedLead.getLeadId());
                    return newView;
                });

        userLeadView.setLastLeadViewed(createdOnTime);

        userLeadViewRepository.save(userLeadView);

        logger.info("Lead created by {}: Comment Time={} | View Time={}",
                username,
                comment.getCreatedOn(),
                userLeadView.getLastLeadViewed());
        return transformToLeadDTO(savedLead);

    }
    return transformToLeadDTO(leadRepository.save(lead));
}

    public String buildPathForLead(Long leadId) {
        return leadRepository.findById(leadId)
                .map(lead -> {
                    String workspaceName = lead.getCampaign().getWorkspace().getWorkspaceName();
                    String campaignName = lead.getCampaign().getCampaignName();

                    return workspaceName + " > " + campaignName;
                })
                .orElse("Path not found");
    }


@Transactional
public LeadDTO updateLead(LeadDTO leadDTO)
{
    Optional<Lead> existingLeadOpt = leadRepository.findById(leadDTO.getLeadId());
    if(!existingLeadOpt.isPresent())
    {
        throw new IllegalArgumentException("lead does not exist");
    }
    Lead existingLead = existingLeadOpt.get();

    StringBuilder changes = new StringBuilder();

    String username = RadiusUtil.getCurrentUsername();

    //Archeive Comment
    if (leadDTO.getIsArchived() != null &&
            !leadDTO.getIsArchived().equals(existingLead.getIsArchived())) {

        changes.append(username)
                .append(" ")
                .append(leadDTO.getIsArchived() ? "archived" : "unarchived")
                .append(" this Campaign.");
    }


    boolean leadNameChanged = false;
    // Compare name change
    if(existingLead.getLeadName()== null){
        if(leadDTO.getLeadName()!= null){
            changes.append(username)
                    .append(" updated the Title. Old Value: '")
                    .append(existingLead.getLeadName())
                    .append("'  New Value: '")
                    .append(leadDTO.getLeadName()).append("'");

            leadNameChanged = true;
        }
    }else if(leadDTO.getLeadName()== null){
        changes.append(username)
                .append(" updated the Title. Old Value: '")
                .append(existingLead.getLeadName())
                .append("'  New Value: '")
                .append(leadDTO.getLeadName()).append("'");
        leadNameChanged = true;
    }
    else if (!existingLead.getLeadName().equals(leadDTO.getLeadName())) {
        changes.append(username)
                .append(" updated the Title. Old Value: '")
                .append(existingLead.getLeadName())
                .append("'  New Value: '")
                .append(leadDTO.getLeadName()).append("'");
        leadNameChanged = true;
    }

    if (leadNameChanged){

        existingLead.setLeadName(leadDTO.getLeadName());
        existingLead.setLeadTitle(leadDTO.getLeadName());

        // Sync Company Name using your method
        Company company = getCompanyFromLead(existingLead);
        if (company != null) {
            company.setName(leadDTO.getLeadName());
            companyRepository.save(company);
        }

    }
    String oldDescription = existingLead.getDescription();
    String newDescription = leadDTO.getDescription();

// Clean emptiness handling
    boolean isOldDescEmpty = (oldDescription == null || oldDescription.trim().isEmpty() || oldDescription.equalsIgnoreCase("N/A"));
    boolean isNewDescEmpty = (newDescription == null || newDescription.trim().isEmpty());

// Only evaluate if frontend actually sends the field
    if (newDescription != null) {
        if (isOldDescEmpty && !isNewDescEmpty) {
            changes.append(username)
                    .append(" added the Description: '")
                    .append(newDescription)
                    .append("'. ");
        } else if (!isOldDescEmpty && !isNewDescEmpty && !oldDescription.equals(newDescription)) {
            changes.append(username)
                    .append(" updated the Description. Old Value: '")
                    .append(oldDescription)
                    .append("' New Value: '")
                    .append(newDescription)
                    .append("'. ");
        }
    }

    String oldBusinessUnit = existingLead.getBusinessUnit();
    String newBusinessUnit = leadDTO.getBusinessUnit();

// Treat "N/A" and empty as empty
    boolean isOldBUEmpty = (oldBusinessUnit == null || oldBusinessUnit.trim().isEmpty() || oldBusinessUnit.equalsIgnoreCase("N/A"));
    boolean isNewBUEmpty = (newBusinessUnit == null || newBusinessUnit.trim().isEmpty() || newBusinessUnit.equalsIgnoreCase("N/A"));

    if (!isNewBUEmpty) {
        if (isOldBUEmpty) {
            changes.append(username)
                    .append(" added the Business Unit: '")
                    .append(newBusinessUnit)
                    .append("'. ");
        } else if (!oldBusinessUnit.equals(newBusinessUnit)) {
            changes.append(username)
                    .append(" updated the Business Unit. Old Value: '")
                    .append(oldBusinessUnit)
                    .append("' New Value: '")
                    .append(newBusinessUnit)
                    .append("'. ");
        }
    }

    //for lead move comment
    boolean isLeadMoved = false;
    StringBuilder moveLeadComment = new StringBuilder();

// OLD path
    String oldLeadPath = buildLeadPath(existingLead);

// NEW path
    String newLeadPath = buildLeadPath(leadDTO);

    if (!oldLeadPath.equals(newLeadPath)) {
        isLeadMoved = true;
        moveLeadComment.append(username)
                .append(" moved this lead.\n")
                .append("From: ")
                .append(oldLeadPath)
                .append("\nTo: ")
                .append(newLeadPath)
                .append(". ");
    }

// Append into changes so it gets saved
    if (isLeadMoved) {
        changes.append(moveLeadComment);
    }



    Lead lead = transformToLead(leadDTO);

    // Flag to indicate if system comment is created
    Boolean isSystemComment = false;

    if (changes.length() > 0) {
        Comments comment = new Comments();
        comment.setLead(lead);
        comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
        comment.setCommentDescription(changes.toString());
        comment.setCreatedOn(System.currentTimeMillis());
        comment.setIsSystemComment(true);
        commentsRepository.save(comment);

        isSystemComment = true;
    }

    Lead savedLead  = leadRepository.save(lead);

    LeadDTO updatedDTO = transformToLeadDTO(savedLead);

    updatedDTO.setIsSystemComment(isSystemComment);

    return updatedDTO;

}

    private String buildLeadPath(Lead lead) {
        if (lead != null) {
            Campaign campaign = lead.getCampaign();
            Workspace workspace = campaign != null ? campaign.getWorkspace() : null;
            return (workspace != null ? workspace.getWorkspaceName() : "Unknown Workspace")
                    + " > "
                    + (campaign != null ? campaign.getCampaignName() : "Unknown Campaign");
        }
        return "Unknown";
    }

    private String buildLeadPath(LeadDTO leadDTO) {
        if (leadDTO.getCampaignId() != null) {
            Campaign campaign = campaignRepository.findById(leadDTO.getCampaignId())
                    .orElse(null);
            if (campaign != null) {
                Workspace workspace = campaign.getWorkspace();
                return (workspace != null ? workspace.getWorkspaceName() : "Unknown Workspace")
                        + " > "
                        + (campaign.getCampaignName() != null ? campaign.getCampaignName() : "Unknown Campaign");
            }
        }
        return "Unknown";
    }


    public Optional<Lead> getLeadById(Long id)
    {
        return leadRepository.findById(id);
    }

    public List<LeadDTO> getLeadByCampaignId(Long campaignId, String userName)
    {
        List<Lead> allLead = leadRepository.findByCampaign_CampaignId(campaignId);

        List<UserLeadSequence> userLeadSequences = userLeadSequenceRepository.findByUserName(userName);
        // Convert user-specific sequences to a Map for easy access
        Map<Long, Integer> userSpecificSequences = userLeadSequences.stream()
                .collect(Collectors.toMap(
                        seq -> seq.getLead().getLeadId(),
                        UserLeadSequence::getSequenceOrder
                ));
        return allLead.stream()
                .sorted((c1, c2) -> {
                    // Get the user-specific sequence or default if not found
                    int seq1 = userSpecificSequences.getOrDefault(c1.getLeadId(), getDefaultSequenceOrder());
                    int seq2 = userSpecificSequences.getOrDefault(c2.getLeadId(), getDefaultSequenceOrder());
                    // Compare the two sequence orders
                    return Integer.compare(seq1, seq2);
                }).map(lead -> {
                    LeadDTO dto = transformToLeadDTO(lead);
                    dto.setPath(buildFullLeadPath(lead)); // Added the path here
                    return dto;
                })
                .toList();
    }

    public boolean deleteLead(Long id)
    {
        if(leadRepository.existsById(id))
        {
            leadRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public void saveLastReadForLead(String userName, Long leadId, Long lastLeadViewed) {

        // First, check if the lead exists
        Optional<Lead> leadOptional = leadRepository.findById(leadId);
        if (!leadOptional.isPresent()) {
            throw new IllegalArgumentException("Lead not found for ID: " + leadId);
        }

        // Check if an entry already exists
        Optional<UserLeadView> existing =
                userLeadViewRepository.findByUserNameAndLeadId(userName, leadId);

        UserLeadView entry;
        if (existing.isPresent()) {
            entry = existing.get();
        } else {
            entry = new UserLeadView();
            entry.setUserName(userName);
            entry.setLeadId(leadId);
        }

        // Update last viewed timestamp
        entry.setLastLeadViewed(lastLeadViewed);
        userLeadViewRepository.save(entry);
    }

    public Lead transformToLead(LeadDTO leadDTO)
    {
        Optional<Campaign>campaign = campaignRepository.findById(leadDTO.getCampaignId());
        if(campaign.isPresent())
        {
            Lead lead;
            if(leadDTO.getLeadId()!= null)
            {
                Optional<Lead> leadOptional = leadRepository.findById(leadDTO.getLeadId());
                if(leadOptional.isPresent()) {
                    lead = leadOptional.get();
                }else{
                    lead = new Lead();
                }

            }else
            {
                 lead = new Lead();
            }
            if(lead.getCreatedOn()== null)
            {
                lead.setCreatedOn(System.currentTimeMillis());
            }
//            lead.setLeadName(leadDTO.getLeadName());
//            lead.setLeadTitle(leadDTO.getLeadTitle());
//            lead.setDescription(leadDTO.getDescription());
//            lead.setCampaign(campaign.get());
//            lead.setBusinessUnit(leadDTO.getBusinessUnit());

            // Update only if not null in DTO, else preserve existing
            if (leadDTO.getLeadName() != null) {
                lead.setLeadName(leadDTO.getLeadName());
            }
            if (leadDTO.getLeadTitle() != null) {
                lead.setLeadTitle(leadDTO.getLeadTitle());
            }
            if (leadDTO.getDescription() != null) {
                lead.setDescription(leadDTO.getDescription());
            }
            if (leadDTO.getBusinessUnit() != null) {
                lead.setBusinessUnit(leadDTO.getBusinessUnit());
            }

            // Handle archive status
            lead.setIsArchived(leadDTO.getIsArchived() != null ?
                    leadDTO.getIsArchived() : false);

            // Always update campaign (since move needs this)
            lead.setCampaign(campaign.get());

            // Defensive check: default to false if null
            lead.setIsSystemComment(leadDTO.getIsSystemComment() != null && leadDTO.getIsSystemComment());

            return lead;
        }
        else {
            throw new IllegalArgumentException("Campaign ID not found");
        }
    }
    public LeadDTO transformToLeadDTO(Lead lead)
    {
        LeadDTO leadDTO = new LeadDTO();
        leadDTO.setLeadId(lead.getLeadId());
        leadDTO.setLeadName(lead.getLeadName());
        leadDTO.setLeadTitle(lead.getLeadTitle());
        leadDTO.setDescription(lead.getDescription());
        leadDTO.setCampaignId(lead.getCampaign().getCampaignId());
        // Add archive status to DTO
        leadDTO.setIsArchived(lead.getIsArchived());
        leadDTO.setBusinessUnit(lead.getBusinessUnit());
        if(lead.getCreatedOn()!= null) {
            leadDTO.setCreatedOn(lead.getCreatedOn());
        }
        leadDTO.setPath(buildFullLeadPath(lead));
        return leadDTO;
    }

    public String buildFullLeadPath(Lead lead) {
        if (lead == null) return "Unknown";

        Campaign campaign = lead.getCampaign();
        Workspace workspace = campaign != null ? campaign.getWorkspace() : null;

        // Find active lifecycle for this lead
        String lifecycleName = "Unknown Lifecycle";
        if (lead.getLeadlifecycleList() != null && !lead.getLeadlifecycleList().isEmpty()) {
            lifecycleName = lead.getLeadlifecycleList().stream()
                    .filter(l -> "active".equalsIgnoreCase(l.getStatus()))
                    .map(Lifecycle::getLifecycleName)
                    .findFirst()
                    .orElse(lifecycleName);
        }

        return (workspace != null ? workspace.getWorkspaceName() : "Unknown Workspace")
                + " > "
                + (campaign != null ? campaign.getCampaignName() : "Unknown Campaign")
                + " > "
                + lifecycleName;
    }


    public int getDefaultSequenceOrder() {
        return 999; // Example of default sequence order, change as per logic
    }
    public List<UserLeadSequence> updateLeadSequence(String userName, List<LeadDTO> orderedLeadList)
    {
        int leadLength = orderedLeadList.size();
        List<UserLeadSequence> sequencesToSave = new ArrayList<>();

        List<Long> leadIds = orderedLeadList.stream()
                .map(LeadDTO::getLeadId).toList();

        List<UserLeadSequence> existingSequences = userLeadSequenceRepository.findByUserNameAndLead_LeadIdIn(userName, leadIds);

        Map<Long, UserLeadSequence> sequenceMap = existingSequences.stream()
                .collect(Collectors.toMap(seq -> seq.getLead().getLeadId(), seq -> seq));

        for(int i = 0; i < leadLength ; i++)
        {
            Long leadId = orderedLeadList.get(i).getLeadId();
            int newSequenceOrder = i + 1;

            UserLeadSequence userLeadSequence = sequenceMap.get(leadId);

            if (userLeadSequence != null) {
                // Update the sequence order if needed
                if (userLeadSequence.getSequenceOrder() != newSequenceOrder) {
                    userLeadSequence.setSequenceOrder(newSequenceOrder);
                    sequencesToSave.add(userLeadSequence);
                }
            } else {
                // If no sequence exists, create a new one
                Optional<Lead> lead = leadRepository.findById(leadId);
                if (lead.isPresent()) {
                    UserLeadSequence newLeadSequence = new UserLeadSequence();
                    newLeadSequence.setLead(lead.get());
                    newLeadSequence.setSequenceOrder(newSequenceOrder);
                    newLeadSequence.setUserName(userName);
                    sequencesToSave.add(newLeadSequence);
                }
            }
        }
        if (!sequencesToSave.isEmpty()) {
            userLeadSequenceRepository.saveAll(sequencesToSave);
        }
        return sequencesToSave;
    }

    // In code we specify only phone outreachh task but it works for all task type cause the same contact is shared among all tasks.
    private Company getCompanyFromLead(Lead lead) {

        if (lead.getLeadlifecycleList() == null) {
            return null;
        }

        for (Lifecycle lifecycle : lead.getLeadlifecycleList()) {
            if (lifecycle.getTaskList() == null) {
                continue;
            }

            for (Task task : lifecycle.getTaskList()) {
                if (task instanceof PhoneOutreachTask) {
                    PhoneOutreachTask phoneTask = (PhoneOutreachTask) task;

                    if (phoneTask.getRelatedContact() != null &&
                            phoneTask.getRelatedContact().getCompany() != null) {

                        return phoneTask.getRelatedContact().getCompany();
                    }
                }
            }
        }
        return null;
    }
}
