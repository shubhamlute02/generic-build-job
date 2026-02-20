package com.accrevent.radius.service;

import com.accrevent.radius.dto.CampaignSpecificationDTO;
import com.accrevent.radius.dto.TaskDTO;
import com.accrevent.radius.dto.TaskWeeklyPlannerDTO;
import com.accrevent.radius.model.*;
import com.accrevent.radius.repository.*;
import com.accrevent.radius.dto.CampaignDTO;
import com.accrevent.radius.util.CampaignType;
import com.accrevent.radius.util.LifecycleName;
import com.accrevent.radius.util.RadiusConstants;
import com.accrevent.radius.util.RadiusUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.accrevent.radius.util.RadiusUtil.formatter;
import static com.accrevent.radius.util.RadiusUtil.stringtoZonedDateTime;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ConstantLifecycleRepository constantLifecycleRepository;
    private final UserCampaignSequenceRepository userCampaignSequenceRepository;
    private final CommentsRepository commentsRepository;
    private final CampaignSpecificationRepository campaignSpecificationRepository;
    private final UserCampaignSpecSeqRepository userCampaignSpecSeqRepository;
    private final TaskRepository taskRepository;
    public CampaignService(CampaignRepository campaignRepository,
                           WorkspaceRepository workspaceRepository,
                           ConstantLifecycleRepository constantLifecycleRepository,
                           UserCampaignSequenceRepository userCampaignSequenceRepository,
                           CommentsRepository commentsRepository,
                           CampaignSpecificationRepository campaignSpecificationRepository,
                           UserCampaignSpecSeqRepository userCampaignSpecSeqRepository, TaskRepository taskRepository)
    {

        this.campaignRepository = campaignRepository;
        this.workspaceRepository = workspaceRepository;
        this.constantLifecycleRepository = constantLifecycleRepository;
        this.userCampaignSequenceRepository = userCampaignSequenceRepository;
        this.commentsRepository = commentsRepository;
        this.campaignSpecificationRepository = campaignSpecificationRepository;
        this.userCampaignSpecSeqRepository = userCampaignSpecSeqRepository;
        this.taskRepository = taskRepository;
    }

//    private static final DateTimeFormatter DATE_FORMATTER =
//            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public CampaignSpecificationDTO createCampaignSpecification(CampaignSpecificationDTO campaignSpecificationDTO)
    {
        CampaignSpecification campaignSpecification = transformToCampaignSpecification(campaignSpecificationDTO);
        return transformToCampaignSpecificationDTO(campaignSpecificationRepository.save(campaignSpecification));
    }


//    public CampaignDTO createCampaign(CampaignDTO campaignDTO)
//    {
//        Campaign campaign = transformToCampaign(campaignDTO);
//        if(campaign.getCampaignId()== null) {
//            List<Lifecycle> lifecycleList = new ArrayList<>();
//            AtomicBoolean flag = new AtomicBoolean(true);
//            List<ConstantLifecycle> constantLifecycles = constantLifecycleRepository.findByCycleId(1L);
//            constantLifecycles.forEach(constantLifecycle -> {
//                Lifecycle lifecycle = new Lifecycle();
//                lifecycle.setLifecycleName(constantLifecycle.getCycleName());
//                lifecycle.setStatus("inActive");
//                if(flag.get())
//                {
//                    lifecycle.setStatus("active");
//                    flag.set(false);
//                }
//                lifecycle.setCampaign(campaign);
//                lifecycleList.add(lifecycle);
//            });
//            campaign.setLifecycleList(lifecycleList);
//        }
//        return transformToCampaignDTO(campaignRepository.save(campaign));
//    }

//    public CampaignDTO createCampaign(CampaignDTO dto) {
//        Campaign campaign = transformToCampaign(dto);
//
//        if (campaign.getCampaignId() == null){
//            List<Lifecycle> lifecycleList = new ArrayList<>();
//            AtomicBoolean first = new AtomicBoolean(true);
//
//            long cycleId = CampaignType.OUTREACH.equals(dto.getType()) ? 4L : 1L;
//            List<ConstantLifecycle> constants = constantLifecycleRepository.findByCycleId(cycleId);
//
//            constants.forEach(cl -> {
//                Lifecycle lc = new Lifecycle();
//                lc.setLifecycleName(cl.getCycleName());
//                lc.setStatus(first.get() ? "active" : "inActive");
//                first.set(false);
//                lc.setCampaign(campaign);
//                lc.setType(dto.getType());
//                lifecycleList.add(lc);
//            });
//
//            campaign.setLifecycleList(lifecycleList);
//        }
//
//        Campaign saved = campaignRepository.save(campaign);
//        return transformToCampaignDTO(saved);
//    }

    public CampaignDTO createCampaign(CampaignDTO dto) {
        Campaign campaign = transformToCampaign(dto);

        if (campaign.getCampaignId() == null){
            List<Lifecycle> lifecycleList = new ArrayList<>();
            AtomicBoolean first = new AtomicBoolean(true);

            long cycleId = CampaignType.OUTREACH.equals(dto.getType()) ? 4L : 1L;
            List<ConstantLifecycle> constants = constantLifecycleRepository.findByCycleId(cycleId);

            constants.forEach(cl -> {
                Lifecycle lc = new Lifecycle();
                lc.setLifecycleName(cl.getCycleName());
                lc.setStatus(first.get() ? "active" : "inActive");
                first.set(false);
                lc.setCampaign(campaign);
                lc.setType(dto.getType());
                lifecycleList.add(lc);
            });

            campaign.setLifecycleList(lifecycleList);
        }

        // Save campaign first to generate ID
        Campaign savedCampaign = campaignRepository.save(campaign);

        // Generate system-generated comment for campaign creation
        String username = RadiusUtil.getCurrentUsername();

        StringBuilder commentBuilder = new StringBuilder();
        commentBuilder.append(username)
                .append(" created the campaign.");

        Comments comment = new Comments();
        comment.setCampaign(savedCampaign);
        comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
        comment.setCommentDescription(commentBuilder.toString());
        comment.setCreatedOn(System.currentTimeMillis());
        comment.setIsSystemComment(true);
        commentsRepository.save(comment);
        return transformToCampaignDTO(campaignRepository.save(campaign));
    }



//    @Transactional
//    public CampaignDTO updateCampaign(CampaignDTO campaignDTO)
//    {
//        Optional<Campaign> existingCampaignOpt = campaignRepository.findById(campaignDTO.getCampaignId());
//
//        Campaign existingCampaign = existingCampaignOpt.get();
//
//        StringBuilder changes = new StringBuilder();
//
//        String username = RadiusUtil.getCurrentUsername();
//
//        // Compare name change
//        if(existingCampaign.getCampaignName() == null){
//            if(campaignDTO.getCampaignName() != null){
//                changes.append(username)
//                        .append(" has updated the campaign name from '")
//                        .append(existingCampaign.getCampaignName())
//                        .append("' to '")
//                        .append(campaignDTO.getCampaignName()).append("'");
//            }
//        }else if(campaignDTO.getCampaignName()== null){
//            changes.append(username)
//                    .append(" has updated the campaign name from '")
//                    .append(existingCampaign.getCampaignName())
//                    .append("' to '")
//                    .append(campaignDTO.getCampaignName()).append("'");
//        }
//        else if (!existingCampaign.getCampaignName().equals(campaignDTO.getCampaignName())) {
//            changes.append(username)
//                    .append(" has updated the campaign name from '")
//                    .append(existingCampaign.getCampaignName())
//                    .append("' to '")
//                    .append(campaignDTO.getCampaignName()).append("'");
//        }
//
//        // Compare description change
//        if(existingCampaign.getDescription() == null){
//            if(campaignDTO.getDescription()!= null){
//                changes.append(username)
//                        .append(" has updated the description from '")
//                        .append(existingCampaign.getDescription())
//                        .append("' to '")
//                        .append(campaignDTO.getDescription()).append("'");
//            }
//        }else if(campaignDTO.getDescription()== null){
//            changes.append(username)
//                    .append(" has updated the description from '")
//                    .append(existingCampaign.getDescription())
//                    .append("' to '")
//                    .append(campaignDTO.getDescription()).append("'");
//        }
//        else if (!existingCampaign.getDescription().equals(campaignDTO.getDescription())) {
//            changes.append(username)
//                    .append(" has updated the description from '")
//                    .append(existingCampaign.getDescription())
//                    .append("' to '")
//                    .append(campaignDTO.getDescription()).append("'");
//        }
//
//        // Compare Owner change
//        if(existingCampaign.getOwner() == null){
//            if(campaignDTO.getOwner()!= null){
//                changes.append(username)
//                        .append(" has changed the owner from '")
//                        .append(existingCampaign.getOwner())
//                        .append("' to '")
//                        .append(campaignDTO.getOwner()).append("'");
//            }
//        }else if(campaignDTO.getOwner()== null){
//            changes.append(username)
//                    .append(" has changed the owner from '")
//                    .append(existingCampaign.getOwner())
//                    .append("' to '")
//                    .append(campaignDTO.getOwner()).append("'");
//        }
//        else if (!existingCampaign.getOwner().equals(campaignDTO.getOwner())) {
//            changes.append(username)
//                    .append(" has updated the owner from '")
//                    .append(existingCampaign.getOwner())
//                    .append("' to '")
//                    .append(campaignDTO.getOwner()).append("'");
//        }
//
//        if(!existingCampaign.getWorkspace().getWorkspaceId().equals(campaignDTO.getWorkspaceId()))
//        {
//            changes.append(username)
//                    .append(" has updated the Workspace from '")
//                    .append(existingCampaign.getWorkspace().getWorkspaceId())
//                    .append("' to '")
//                    .append(campaignDTO.getWorkspaceId()).append("'");
//        }
//
//
//        // Flag to indicate if system comment is created
//        Boolean isSystemComment = false;
//
//        Campaign campaign = transformToCampaign(campaignDTO);
//
//        if (changes.length() > 0) {
//            Comments comment = new Comments();
//            comment.setCampaign(campaign);
////            comment.setCreatedBy(username);
//            comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
//            comment.setCommentDescription(changes.toString());
//            comment.setCreatedOn(System.currentTimeMillis());
//            comment.setIsSystemComment(true);
//            commentsRepository.save(comment);
//
//            isSystemComment = true;
//        }
//
//        Campaign savedCampaign  = campaignRepository.save(campaign);
//
//        CampaignDTO updatedDTO = transformToCampaignDTO(savedCampaign);
//
//        updatedDTO.setIsSystemComment(isSystemComment);
//
//        return updatedDTO;
//
//
//    }


@Transactional
public CampaignDTO updateCampaign(CampaignDTO campaignDTO)
{
    Optional<Campaign> existingCampaignOpt = campaignRepository.findById(campaignDTO.getCampaignId());

    Campaign existingCampaign = existingCampaignOpt.get();

    StringBuilder changes = new StringBuilder();

    String username = RadiusUtil.getCurrentUsername();
    System.out.println("username"+username);


    //Archeive Comment
    if (campaignDTO.getIsArchived() != null &&
            !campaignDTO.getIsArchived().equals(existingCampaign.getIsArchived())) {

        changes.append(username)
                .append(" ")
                .append(campaignDTO.getIsArchived() ? "archived" : "unarchived")
                .append(" this Campaign.");
    }

    // Compare name change
    if(existingCampaign.getCampaignName() == null){
        if(campaignDTO.getCampaignName() != null){
            changes.append(username)
                    .append(" updated the Title.\n")
                    .append("Old Value: '")
                    .append(existingCampaign.getCampaignName())
                    .append("'\n")
                    .append(" New Value: '")
                    .append(campaignDTO.getCampaignName()).append("'");
        }
    }else if(campaignDTO.getCampaignName()== null){
        changes.append(username)
                .append(" updated the Title.\n")
                .append("Old Value: '")
                .append(existingCampaign.getCampaignName())
                .append("'\n")
                .append(" New Value: '")
                .append(campaignDTO.getCampaignName()).append("'");
    }
    else if (!existingCampaign.getCampaignName().equals(campaignDTO.getCampaignName())) {
        changes.append(username)
                .append(" updated the Title.\n")
                .append("Old Value: '")
                .append(existingCampaign.getCampaignName())
                .append("'\n")
                .append(" New Value: '")
                .append(campaignDTO.getCampaignName()).append("'");
    }

    // Compare description change
    if ((existingCampaign.getDescription() == null || existingCampaign.getDescription().isEmpty())
            && campaignDTO.getDescription() != null && !campaignDTO.getDescription().isEmpty()) {
        changes.append(username)
                .append(" added a Description: ")
                .append(campaignDTO.getDescription())
                .append(". ");


    }else if(campaignDTO.getDescription()== null){
        changes.append(username)
                .append(" updated the Description.\n")
                .append(" Old Value: '")
                .append(existingCampaign.getDescription())
                .append("'\n")
                .append("New Value: '")
                .append(campaignDTO.getDescription()).append("'");
    }
    else if (!existingCampaign.getDescription().equals(campaignDTO.getDescription())) {
        changes.append(username)
                .append(" updated the Description.\n")
                .append(" Old Value: '")
                .append(existingCampaign.getDescription())
                .append("'\n")
                .append("New Value: '")
                .append(campaignDTO.getDescription()).append("'");
    }

    // Compare Owner change
    if(existingCampaign.getOwner() == null){
        if(campaignDTO.getOwner()!= null){
            changes.append(username)
                    .append(" changed the Owner from '")
                    .append(existingCampaign.getOwner())
                    .append("' to '")
                    .append(campaignDTO.getOwner()).append("'");
        }
    }else if(campaignDTO.getOwner()== null){
        changes.append(username)
                .append(" changed the Owner from '")
                .append(existingCampaign.getOwner())
                .append("' to '")
                .append(campaignDTO.getOwner()).append("'");
    }
    else if (!existingCampaign.getOwner().equals(campaignDTO.getOwner())) {
        changes.append(username)
                .append(" changed the Owner from '")
                .append(existingCampaign.getOwner())
                .append("' to '")
                .append(campaignDTO.getOwner()).append("'");
    }

    //compare for change in planned Start date
    // Compare Planned Start Date changes
    if (existingCampaign.getPlannedStartDate() != null && campaignDTO.getPlannedStartDate() != null) {
        if (!existingCampaign.getPlannedStartDate().equals(campaignDTO.getPlannedStartDate())) {
            String oldDateFormatted = Instant.ofEpochMilli(existingCampaign.getPlannedStartDate())
                    .atZone(ZoneId.systemDefault())
//                    .toLocalDateTime()
                    .format(RadiusUtil.COMMENT_DATE_FORMATTER);

            String newDateFormatted = Instant.ofEpochMilli(campaignDTO.getPlannedStartDate())
                    .atZone(ZoneId.systemDefault())
//                    .toLocalDateTime()
                    .format(RadiusUtil.COMMENT_DATE_FORMATTER);

            changes.append(username)
                    .append(" changed the Planned Start Date from  ")
                    .append(oldDateFormatted)
                    .append(" to ")
                    .append(newDateFormatted)
                    .append(". ");
        }
    } else if (existingCampaign.getPlannedStartDate() == null && campaignDTO.getPlannedStartDate() != null) {
        String newDateFormatted = Instant.ofEpochMilli(campaignDTO.getPlannedStartDate())
                .atZone(ZoneId.systemDefault())
//                .toLocalDateTime()
                .format(RadiusUtil.COMMENT_DATE_FORMATTER);

        changes.append(username)
                .append(" added Planned Start Date as ")
                .append(newDateFormatted)
                .append(". ");
    }

    //For comparing plannedEnd date
    // Compare Planned End Date changes
    if (existingCampaign.getPlannedEndDate() != null && campaignDTO.getPlannedEndDate() != null) {
        if (!existingCampaign.getPlannedEndDate().equals(campaignDTO.getPlannedEndDate())) {
            String oldDateFormatted = Instant.ofEpochMilli(existingCampaign.getPlannedEndDate())
                    .atZone(ZoneId.systemDefault())
//                    .toLocalDateTime()
                    .format(RadiusUtil.COMMENT_DATE_FORMATTER);

            String newDateFormatted = Instant.ofEpochMilli(campaignDTO.getPlannedEndDate())
                    .atZone(ZoneId.systemDefault())
//                    .toLocalDateTime()
                    .format(RadiusUtil.COMMENT_DATE_FORMATTER);

            changes.append(username)
                    .append(" changed the Planned End Date from ")
                    .append(oldDateFormatted)
                    .append(" to ")
                    .append(newDateFormatted)
                    .append(". ");
        }
    } else if (existingCampaign.getPlannedEndDate() == null && campaignDTO.getPlannedEndDate() != null) {
        String newDateFormatted = Instant.ofEpochMilli(campaignDTO.getPlannedEndDate())
                .atZone(ZoneId.systemDefault())
//                .toLocalDateTime()
                .format(RadiusUtil.COMMENT_DATE_FORMATTER);

        changes.append(username)
                .append(" added Planned End Date as ")
                .append(newDateFormatted)
                .append(". ");
    }

    //Actual Start date
    // Compare Actual Start Date changes
    if (existingCampaign.getActualStartDate() != null && campaignDTO.getActualStartDate() != null) {
        if (!existingCampaign.getActualStartDate().equals(campaignDTO.getActualStartDate())) {
            String oldDateFormatted = Instant.ofEpochMilli(existingCampaign.getActualStartDate())
                    .atZone(ZoneId.systemDefault())
//                    .toLocalDateTime()
                    .format(RadiusUtil.COMMENT_DATE_FORMATTER);

            String newDateFormatted = Instant.ofEpochMilli(campaignDTO.getActualStartDate())
                    .atZone(ZoneId.systemDefault())
//                    .toLocalDateTime()
                    .format(RadiusUtil.COMMENT_DATE_FORMATTER);

            changes.append(username)
                    .append(" changed the Actual Start Date from ")
                    .append(oldDateFormatted)
                    .append(" to ")
                    .append(newDateFormatted)
                    .append(". ");
        }
    } else if (existingCampaign.getActualStartDate() == null && campaignDTO.getActualStartDate() != null) {
        String newDateFormatted = Instant.ofEpochMilli(campaignDTO.getActualStartDate())
                .atZone(ZoneId.systemDefault())
//                .toLocalDateTime()
                .format(RadiusUtil.COMMENT_DATE_FORMATTER);

        changes.append(username)
                .append(" added Actual Start Date as ")
                .append(newDateFormatted)
                .append(". ");
    }

    //Actual End date
    // Compare Actual End Date changes
    if (existingCampaign.getActualEndDate() != null && campaignDTO.getActualEndDate() != null) {
        if (!existingCampaign.getActualEndDate().equals(campaignDTO.getActualEndDate())) {
            String oldDateFormatted = Instant.ofEpochMilli(existingCampaign.getActualEndDate())
                    .atZone(ZoneId.systemDefault())
//                    .toLocalDateTime()
                    .format(RadiusUtil.COMMENT_DATE_FORMATTER);

            String newDateFormatted = Instant.ofEpochMilli(campaignDTO.getActualEndDate())
                    .atZone(ZoneId.systemDefault())
//                    .toLocalDateTime()
                    .format(RadiusUtil.COMMENT_DATE_FORMATTER);

            changes.append(username)
                    .append(" changed the Actual End Date from ")
                    .append(oldDateFormatted)
                    .append(" to ")
                    .append(newDateFormatted)
                    .append(". ");
        }
    } else if (existingCampaign.getActualEndDate() == null && campaignDTO.getActualEndDate() != null) {
        String newDateFormatted = Instant.ofEpochMilli(campaignDTO.getActualEndDate())
                .atZone(ZoneId.systemDefault())
//                .toLocalDateTime()
                .format(RadiusUtil.COMMENT_DATE_FORMATTER);

        changes.append(username)
                .append(" added Actual End Date as ")
                .append(newDateFormatted)
                .append(". ");
    }



    if(!existingCampaign.getWorkspace().getWorkspaceId().equals(campaignDTO.getWorkspaceId()))
    {
        changes.append(username)
                .append(" has updated the Workspace from '")
                .append(existingCampaign.getWorkspace().getWorkspaceId())
                .append("' to '")
                .append(campaignDTO.getWorkspaceId()).append("'");
    }


    // Flag to indicate if system comment is created
    Boolean isSystemComment = false;

    Campaign campaign = transformToCampaign(campaignDTO);

    if (changes.length() > 0) {
        Comments comment = new Comments();
        comment.setCampaign(campaign);
//        comment.setCreatedBy(username);
        comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
        comment.setCommentDescription(changes.toString());
        comment.setCreatedOn(System.currentTimeMillis());
        comment.setIsSystemComment(true);
        commentsRepository.save(comment);

        isSystemComment = true;
    }

    Campaign savedCampaign  = campaignRepository.save(campaign);

    CampaignDTO updatedDTO = transformToCampaignDTO(savedCampaign);

    updatedDTO.setIsSystemComment(isSystemComment);

    return updatedDTO;


}


    public List<Campaign> getAllCampaign()
    {
        return campaignRepository.findAll();
    }

    public Optional<Campaign> getCampaignById(Long id)
    {

        return campaignRepository.findById(id);
    }

    public List<CampaignDTO> getCampaignByWorkspaceId(Long workspaceId)
    {
        List<CampaignDTO> campaignDTOList = new ArrayList<>();
        campaignRepository.findByWorkspace_WorkspaceId(workspaceId).forEach(campaign -> {
            campaignDTOList.add(transformToCampaignDTO(campaign));
        });
        return campaignDTOList;
    }

    public boolean deleteCampaign(Long id)
    {
        if(campaignRepository.existsById(id))
        {
            campaignRepository.deleteById(id);
            return true;
        }
        return false;
    }


    public List<CampaignDTO> getCampaignsByType(String type) {
        List<Campaign> campaigns = campaignRepository.findByType(type);

        List<String> allowedStates = Arrays.asList(
                LifecycleName.IN_PROGRESS,
                LifecycleName.COMPLETED
        );

        List<Campaign> filteredCampaigns = campaigns.stream()
                .filter(campaign -> {
                    String activeLifecycleName = getActiveLifecycleName(campaign);
                    return activeLifecycleName != null && allowedStates.contains(activeLifecycleName);
                })
                .collect(Collectors.toList());

        return filteredCampaigns.stream()
                .map(this::transformToCampaignDTO)
                .collect(Collectors.toList());
    }

    private String getActiveLifecycleName(Campaign campaign) {
        if (campaign == null || campaign.getLifecycleList() == null || campaign.getLifecycleList().isEmpty()) {
            return null;
        }

        Lifecycle active = campaign.getLifecycleList()
                .stream()
                .filter(lc -> lc.getStatus() != null && "active".equalsIgnoreCase(lc.getStatus()))
                .findFirst()
                .orElse(null);

        return active != null ? active.getLifecycleName() : null;
    }



    public List<CampaignDTO> getOutreachCampaignsByWorkspaceId(Long workspaceId) {
        return campaignRepository.findByWorkspace_WorkspaceIdAndType(workspaceId, CampaignType.OUTREACH)
                .stream()
                .map(this::transformToCampaignDTO)
                .collect(Collectors.toList());
    }



    public boolean deleteCampaignSpecification(Long id)
    {
        if(campaignSpecificationRepository.existsById(id)) {
            campaignSpecificationRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<UserCampaignSequence> updateCampaignSequence(String userName, List<CampaignDTO> orderedCampaignDTOList)
    {
        int campaignLength = orderedCampaignDTOList.size();
        List<UserCampaignSequence> sequencesToSave = new ArrayList<>();

        List<Long> campaignIds = orderedCampaignDTOList.stream()
                .map(CampaignDTO::getCampaignId)
                .collect(Collectors.toList());

        List<UserCampaignSequence> existingSequences = userCampaignSequenceRepository.findByUserNameAndCampaign_CampaignIdIn(userName, campaignIds);

        Map<Long, UserCampaignSequence> sequenceMap = existingSequences.stream()
                .collect(Collectors.toMap(seq -> seq.getCampaign().getCampaignId(), seq -> seq));

        for(int i = 0; i < campaignLength ; i++)
        {
            Long campaignId = orderedCampaignDTOList.get(i).getCampaignId();
            int newSequenceOrder = i + 1;

            UserCampaignSequence userCampaignSequence = sequenceMap.get(campaignId);

            if (userCampaignSequence != null) {
                // Update the sequence order if needed
                if (userCampaignSequence.getSequenceOrder() != newSequenceOrder) {
                    userCampaignSequence.setSequenceOrder(newSequenceOrder);
                    sequencesToSave.add(userCampaignSequence);
                }
            } else {
                // If no sequence exists, create a new one
                Optional<Campaign> campaign = campaignRepository.findById(campaignId);
                if (campaign.isPresent()) {
                    UserCampaignSequence newCampaignSequence = new UserCampaignSequence();
                    newCampaignSequence.setCampaign(campaign.get());
                    newCampaignSequence.setSequenceOrder(newSequenceOrder);
                    newCampaignSequence.setUserName(userName);
                    sequencesToSave.add(newCampaignSequence);
                }
            }
        }
        if (!sequencesToSave.isEmpty()) {
            userCampaignSequenceRepository.saveAll(sequencesToSave);
        }
        return sequencesToSave;
    }


    private Campaign transformToCampaign(CampaignDTO campaignDTO)
    {
        Optional<Workspace>workspace = workspaceRepository.findById(campaignDTO.getWorkspaceId());
        if(workspace.isPresent())
        {
            Campaign campaign = new Campaign();
            if(campaignDTO.getCampaignId()!= null)
            {
                Optional<Campaign> campaignOptional = campaignRepository.findById(campaignDTO.getCampaignId());
                if(campaignOptional.isPresent())
                {
                    campaign = campaignOptional.get();
                }else {
                    campaign = new Campaign();
                }
            }else {
                campaign = new Campaign();
            }
            campaign.setCampaignName(campaignDTO.getCampaignName());
            campaign.setDescription(campaignDTO.getDescription());
            campaign.setWorkspace(workspace.get());
            campaign.setOwner(campaignDTO.getOwner());
            campaign.setActualEndDate(campaignDTO.getActualEndDate());
            campaign.setActualStartDate(campaignDTO.getActualStartDate());
            campaign.setPlannedEndDate(campaignDTO.getPlannedEndDate());
            campaign.setPlannedStartDate(campaignDTO.getPlannedStartDate());

            if (campaignDTO.getType() == null) {
                campaignDTO.setType(CampaignType.DEFAULT);
            }
            if ("outreach".equalsIgnoreCase(campaignDTO.getType())) {
                campaign.setType(CampaignType.OUTREACH);
            } else {
                campaign.setType(CampaignType.DEFAULT);
            }

            // Handle archive status
            campaign.setIsArchived(campaignDTO.getIsArchived() != null ?
                    campaignDTO.getIsArchived() : false);

            // Defensive check: default to false if null
            campaign.setIsSystemComment(campaignDTO.getIsSystemComment() != null && campaignDTO.getIsSystemComment());
            if(campaign.getCreatedOn() == null)
            {
                campaign.setCreatedOn(System.currentTimeMillis());
            }
        return campaign;
        }
        else {
            throw new IllegalArgumentException("Workspace ID not found");
        }
    }

    private CampaignSpecification transformToCampaignSpecification(CampaignSpecificationDTO campaignSpecificationDTO)
    {
        Optional<Campaign> campaign = campaignRepository.findById(campaignSpecificationDTO.getCampaignId());
        if(campaign.isPresent())
        {
            CampaignSpecification campaignSpecification;
            if(campaignSpecificationDTO.getSpecificationId() != null) {
                Optional<CampaignSpecification> campaignSpecificationOptional = campaignSpecificationRepository
                        .findById(campaignSpecificationDTO.getSpecificationId());
                if (campaignSpecificationOptional.isPresent()) {
                    campaignSpecification = campaignSpecificationOptional.get();
                } else {
                    campaignSpecification = new CampaignSpecification();
                }
            }else {
                campaignSpecification = new CampaignSpecification();
            }
            campaignSpecification.setTitle(campaignSpecificationDTO.getTitle());
            campaignSpecification.setDescription(campaignSpecificationDTO.getDescription());
            campaignSpecification.setCampaign(campaign.get());
            return campaignSpecification;
        }
        else {
            throw new IllegalArgumentException("campaign ID not found");
        }
    }
    private CampaignDTO transformToCampaignDTO(Campaign campaign)
    {
        CampaignDTO campaignDTO = new CampaignDTO();
        campaignDTO.setCampaignId(campaign.getCampaignId());
        campaignDTO.setCampaignName(campaign.getCampaignName());
        campaignDTO.setDescription(campaign.getDescription());
        campaignDTO.setWorkspaceId(campaign.getWorkspace().getWorkspaceId());
        campaignDTO.setOwner(campaign.getOwner());
        campaignDTO.setActualEndDate(campaign.getActualEndDate());
        campaignDTO.setActualStartDate(campaign.getActualStartDate());
        campaignDTO.setPlannedEndDate(campaign.getPlannedEndDate());
        campaignDTO.setPlannedStartDate(campaign.getPlannedStartDate());
//        campaignDTO.setOutreach(campaign.getOutreach());
        // Add archive status to DTO
        campaignDTO.setIsArchived(campaign.getIsArchived());

        if (CampaignType.OUTREACH.equals(campaign.getType())) {
            campaignDTO.setType(CampaignType.OUTREACH);
        } else {
            campaignDTO.setType(CampaignType.DEFAULT);
        }



        if(campaign.getCreatedOn()!= null) {
            campaignDTO.setCreatedOn(campaign.getCreatedOn());
        }

        //  Build path: WorkspaceName > LifecycleName
        String path = "";
        if (campaign.getWorkspace() != null) {
            path = campaign.getWorkspace().getWorkspaceName();
        }

        // Assuming Campaign has a list of lifecycles and you want the *active* one
        if (campaign.getLifecycleList() != null && !campaign.getLifecycleList().isEmpty()) {
            Lifecycle activeLifecycle = campaign.getLifecycleList().stream()
                    .filter(lc -> "active".equalsIgnoreCase(lc.getStatus()))
                    .findFirst()
                    .orElse(null);
            if (activeLifecycle != null) {
                path += " > " + activeLifecycle.getLifecycleName();
            }
        }

        campaignDTO.setPath(path);


        return campaignDTO;
    }

    public int getDefaultSequenceOrder() {
        return 999; // Example of default sequence order, change as per logic
    }

    private CampaignSpecificationDTO transformToCampaignSpecificationDTO(CampaignSpecification campaignSpecification)
    {
        CampaignSpecificationDTO campaignSpecificationDTO = new CampaignSpecificationDTO();
        campaignSpecificationDTO.setSpecificationId(campaignSpecification.getSpecificationId());
        campaignSpecificationDTO.setTitle(campaignSpecification.getTitle());
        campaignSpecificationDTO.setDescription(campaignSpecification.getDescription());
        campaignSpecificationDTO.setCampaignId(campaignSpecification.getCampaign().getCampaignId());
        return campaignSpecificationDTO;
    }

    public List<CampaignSpecificationDTO> getCampaignSpecificationByCampaignId(Long campaignId,String userName){
        List<CampaignSpecification> campaignSpecificationList = campaignSpecificationRepository.findByCampaign_CampaignId(campaignId);
        List<UserCampaignSpecSeq> userCampaignSpecSeqs = userCampaignSpecSeqRepository.findByUserName(userName);

        Map<Long, Integer> userSpecificSequences = userCampaignSpecSeqs.stream()
                .collect(Collectors.toMap(
                        seq -> seq.getCampaignSpecification().getSpecificationId(),
                        UserCampaignSpecSeq::getSequenceOrder
                ));

        // Sort by user-specific sequence or default sequence
        List<CampaignSpecification> sortedCampaignSpecs= campaignSpecificationList.stream()
                .sorted((c1, c2) -> {
                    // Get the user-specific sequence or default if not found
                    int seq1 = userSpecificSequences.getOrDefault(c1.getSpecificationId(), getDefaultSequenceOrder());
                    int seq2 = userSpecificSequences.getOrDefault(c2.getSpecificationId(), getDefaultSequenceOrder());
                    // Compare the two sequence orders
                    return Integer.compare(seq1, seq2);
                }).toList();

        List<CampaignSpecificationDTO> campaignSpecificationDTOList = new ArrayList<>();
        sortedCampaignSpecs.forEach(campaignSpecification -> {
            campaignSpecificationDTOList.add(transformToCampaignSpecificationDTO(campaignSpecification));
        });
        return campaignSpecificationDTOList;
    }

    public List<UserCampaignSpecSeq> updateCampaignSpecificSequence(String userName, List<CampaignSpecificationDTO> orderedCampaignSpecDTOList)
    {
        int taskLength = orderedCampaignSpecDTOList.size();

        List<UserCampaignSpecSeq> sequencesToSave = new ArrayList<>();

        List<Long> specificationIds = orderedCampaignSpecDTOList.stream()
                .map(CampaignSpecificationDTO::getSpecificationId).toList();


        List<UserCampaignSpecSeq> existingSequences = userCampaignSpecSeqRepository.findByUserNameAndCampaignSpecification_SpecificationIdIn(userName, specificationIds);

        Map<Long, UserCampaignSpecSeq> sequenceMap = existingSequences.stream()
                .collect(Collectors.toMap(seq -> seq.getCampaignSpecification().getSpecificationId(), seq -> seq));


        for(int i = 0; i < taskLength ; i++)
        {
            Long specificationId = orderedCampaignSpecDTOList.get(i).getSpecificationId();
            int newSequenceOrder = i + 1;

            UserCampaignSpecSeq userTaskSequence = sequenceMap.get(specificationId);

            if (userTaskSequence != null) {
                // Update the sequence order if needed
                if (userTaskSequence.getSequenceOrder() != newSequenceOrder) {
                    userTaskSequence.setSequenceOrder(newSequenceOrder);
                    sequencesToSave.add(userTaskSequence);
                }
            } else {
                // If no sequence exists, create a new one
                Optional<CampaignSpecification> task = campaignSpecificationRepository.findById(specificationId);
                if (task.isPresent()) {
                    UserCampaignSpecSeq newTaskSequence = new UserCampaignSpecSeq();
                    newTaskSequence.setCampaignSpecification(task.get());
                    newTaskSequence.setSequenceOrder(newSequenceOrder);
                    newTaskSequence.setUserName(userName);
                    sequencesToSave.add(newTaskSequence);
                }
            }
        }

        if (!sequencesToSave.isEmpty()) {
            userCampaignSpecSeqRepository.saveAll(sequencesToSave);
        }

        return sequencesToSave;
    }


    public List<CampaignDTO> getCampaignsWithTasksInRange(
            String assignTo,
            long startDate,
            long endDate,
            int todayYear,
            int todayMonth,
            int todayDay) {

        long today = getStartOfDayGMT(todayYear, todayMonth, todayDay);
        List<Campaign> allCampaigns = campaignRepository.findAll(); // fetch all campaigns
        List<CampaignDTO> result = new ArrayList<>();

        for (Campaign campaign : allCampaigns) {

            boolean hasTaskInRange = false;

            // ----- 1. Check campaign lifecycle tasks + subtasks -----
            for (Lifecycle lc : campaign.getLifecycleList()) {
                for (Task t : lc.getTaskList()) {
                    if (isTaskAssignedAndInRange(t, assignTo, startDate, endDate, today)) {
                        hasTaskInRange = true;
                    }
                    if (t.getSubTasks() != null && t.getSubTasks().stream()
                            .anyMatch(st -> isTaskAssignedAndInRange(st, assignTo, startDate, endDate, today))) {
                        hasTaskInRange = true;
                    }
                }
            }

            // ----- 2. Check lead tasks + subtasks (always, independent) -----
            for (Lead lead : campaign.getLeadList()) {

                // fetch all tasks directly under lead
                List<Task> leadTasks = taskRepository.findByLifecycle_Lead_LeadId(lead.getLeadId());

                for (Task t : leadTasks) {
                    if (isTaskAssignedAndInRange(t, assignTo, startDate, endDate, today)) {
                        hasTaskInRange = true;
                    }
                    if (t.getSubTasks() != null && t.getSubTasks().stream()
                            .anyMatch(st -> isTaskAssignedAndInRange(st, assignTo, startDate, endDate, today))) {
                        hasTaskInRange = true;
                    }
                }
            }

            // ----- 3. Add campaign if any task/subtask matched -----
            if (hasTaskInRange) {
                result.add(mapCampaignToDTO(campaign));
            }
        }

        return result;
    }

    private boolean isTaskAssignedAndInRange(Task task, String assignTo, long startDate, long endDate, long today) {
        if (task.getAssignTo() == null || !task.getAssignTo().equals(assignTo)) {
            return false;
        }

        // Exclude tasks whose lifecycle/status is "Completed"
        if (LifecycleName.COMPLETED.equalsIgnoreCase(task.getStatus())) {
            return false;
        }

        return isTaskInDateRange(task.getDueDate(), startDate, endDate, today);
    }


    // Your existing date range logic(it dont return campaign for past task)
//    private boolean isTaskInDateRange(Long taskDueDate, long startDate, long endDate, long today) {
//        if (taskDueDate == null) return false;
//
//        // Past-only range → skip
//        if (endDate < today) return false;
//
//        // Future-only range → include
//        if (startDate > today) return taskDueDate >= startDate && taskDueDate <= endDate;
//
//        // Range includes today → include today + future
//        return taskDueDate >= today && taskDueDate <= endDate;
//    }

    //for including past task for returning campaign
    private boolean isTaskInDateRange(Long taskDueDate, long startDate, long endDate, long today) {
        if (taskDueDate == null) return false;

        // Include past tasks
        if (taskDueDate < today) return true;

        // Include tasks from today to endDate
        return taskDueDate >= today && taskDueDate <= endDate;
    }


    // Map Campaign entity to CampaignDTO
    private CampaignDTO mapCampaignToDTO(Campaign campaign) {
        CampaignDTO dto = new CampaignDTO();
        dto.setCampaignId(campaign.getCampaignId());
        dto.setCampaignName(campaign.getCampaignName());
        dto.setDescription(campaign.getDescription());
        dto.setWorkspaceId(campaign.getWorkspace().getWorkspaceId());
        dto.setCreatedOn(campaign.getCreatedOn());
        dto.setOwner(campaign.getOwner());
        dto.setType("default");
        return dto;
    }

    /**
     * Returns the start of the day at 1:00 AM UTC for the given user date.
     */
    public long getStartOfDayGMT(int year, int month, int day) {
        LocalDate userDate = LocalDate.of(year, month, day);
        return userDate.atTime(1, 0, 0) // day starts at 1 AM UTC
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
    }



}
