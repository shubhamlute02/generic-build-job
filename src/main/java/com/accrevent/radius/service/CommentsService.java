package com.accrevent.radius.service;

import com.accrevent.radius.dto.CommentsDTO;
import com.accrevent.radius.exception.ResourceNotFoundException;
import com.accrevent.radius.model.*;
import com.accrevent.radius.repository.*;
import com.accrevent.radius.util.RadiusUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.accrevent.radius.util.RadiusUtil.formatter;
import static com.accrevent.radius.util.RadiusUtil.getCurrentUsername;

@Service
public class CommentsService {
    private final CommentsRepository commentsRepository;
    private final CampaignRepository campaignRepository;
    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final VersionRepository versionRepository;
    private final TaskRepository taskRepository;
    private final TaskCommentTrackService taskCommentTrackService;
    private final LeadCommentTrackService leadCommentTrackService;
    private final OpportunityCommentTrackService opportunityCommentTrackService;
    private final CampaignCommentTrackService campaignCommentTrackService;

    public CommentsService(CommentsRepository commentsRepository,
                           CampaignRepository campaignRepository,
                           LeadRepository leadRepository,
                           OpportunityRepository opportunityRepository, VersionRepository versionRepository,
                           TaskRepository taskRepository, TaskCommentTrackService taskCommentTrackService, LeadCommentTrackService leadCommentTrackService, OpportunityCommentTrackService opportunityCommentTrackService, CampaignCommentTrackService campaignCommentTrackService)
    {
        this.commentsRepository = commentsRepository;
        this.campaignRepository = campaignRepository;
        this.leadRepository = leadRepository;
        this.opportunityRepository = opportunityRepository;
        this.versionRepository = versionRepository;
        this.taskRepository = taskRepository;
        this.taskCommentTrackService = taskCommentTrackService;
        this.leadCommentTrackService = leadCommentTrackService;
        this.opportunityCommentTrackService = opportunityCommentTrackService;
        this.campaignCommentTrackService = campaignCommentTrackService;
    }

//    public CommentsDTO createComments(CommentsDTO commentsDTO)
//    {
//        Comments comments = transformToComments(commentsDTO);
//        return transformToCommentsDTO(commentsRepository.save(comments));
//    }

    // Create comment for a specific entity
//    public CommentsDTO createCommentForEntity(
//            CommentsDTO commentsDTO,
//            Long campaignId,
//            Long leadId,
//            Long opportunityId,
//            Long taskId) {
//
//        Comments comment = new Comments();
//        comment.setCommentsTitle(commentsDTO.getCommentsTitle());
//        comment.setCommentDescription(commentsDTO.getCommentDescription());
//        comment.setCreatedBy(commentsDTO.getCreatedBy());
//        comment.setCreatedOn(System.currentTimeMillis());
//
//        // Determine which entity to associate the comment with
//        if (campaignId != null) {
//            Campaign campaign = campaignRepository.findById(campaignId)
//                    .orElseThrow(() -> new IllegalArgumentException("Campaign not found with ID: " + campaignId));
//            comment.setCampaign(campaign);
//        } else if (leadId != null) {
//            Lead lead = leadRepository.findById(leadId)
//                    .orElseThrow(() -> new IllegalArgumentException("Lead not found with ID: " + leadId));
//            comment.setLead(lead);
//        } else if (opportunityId != null) {
//            Opportunity opportunity = opportunityRepository.findById(opportunityId)
//                    .orElseThrow(() -> new IllegalArgumentException("Opportunity not found with ID: " + opportunityId));
//            comment.setOpportunity(opportunity);
//        } else if (taskId != null) {
//            Task task = taskRepository.findById(taskId)
//                    .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));
//            comment.setTask(task);
//        } else {
//            throw new IllegalArgumentException("No entity ID provided.");
//        }
//
//        // Save and transform back to DTO
//        Comments savedComment = commentsRepository.save(comment);
//        return transformToCommentsDTO(savedComment);
//    }

    public CommentsDTO createCampaignComment(CommentsDTO commentsDTO) {
        if (commentsDTO.getCampaignId() == null) {
            throw new IllegalArgumentException("campaignId is required");
        }

        Optional<Campaign> campaignOptional = campaignRepository.findById(commentsDTO.getCampaignId());
        if (campaignOptional.isEmpty()) {
            throw new EntityNotFoundException("Campaign not found with id: " + commentsDTO.getCampaignId());
        }

        return saveComment(commentsDTO, commentsDTO.getCampaignId(), null, null, null,null);
    }

    public CommentsDTO createOpportunityComment(CommentsDTO commentsDTO) {
        if (commentsDTO.getOpportunityId() == null) {
            throw new IllegalArgumentException("opportunityId is required");
        }


        Optional<Opportunity> opportunityOptional = opportunityRepository.findById(commentsDTO.getOpportunityId());
        if (opportunityOptional.isEmpty()) {
            throw new EntityNotFoundException("Opportunity not found with id: " + commentsDTO.getOpportunityId());
        }

        return saveComment(commentsDTO, null, commentsDTO.getOpportunityId(), null, null,null);
    }
    public CommentsDTO createLeadComment(CommentsDTO commentsDTO) {
        if (commentsDTO.getLeadId() == null) {
            throw new IllegalArgumentException("leadId is required");
        }

        Optional<Lead> leadOptional = leadRepository.findById(commentsDTO.getLeadId());
        if (leadOptional.isEmpty()) {
            throw new EntityNotFoundException("Lead not found with id: " + commentsDTO.getLeadId());
        }

        return saveComment(commentsDTO, null, null, commentsDTO.getLeadId(), null,null);
    }
    public CommentsDTO createTaskComment(CommentsDTO commentsDTO) {
        if (commentsDTO.getTaskId() == null) {
            throw new IllegalArgumentException("taskId is required");
        }

        Optional<Task> taskOptional = taskRepository.findById(commentsDTO.getTaskId());
        if (taskOptional.isEmpty()) {
            throw new EntityNotFoundException("Task not found with id: " + commentsDTO.getTaskId());
        }

        return saveComment(commentsDTO, null, null, null, commentsDTO.getTaskId(),null);
    }

    public CommentsDTO createVersionComment(CommentsDTO commentsDTO){
        if (commentsDTO.getVersionId() == null) {
            throw new IllegalArgumentException("versionId is required");
        }

        Optional<Version> versionOptional = versionRepository.findById(commentsDTO.getVersionId());
        if (versionOptional.isEmpty()) {
            throw new EntityNotFoundException("Version not found with id: " + commentsDTO.getVersionId());
        }

        return saveComment(commentsDTO, null, null, null,null, commentsDTO.getVersionId());


    }

    public CommentsDTO saveComment(CommentsDTO dto, Long campaignId, Long opportunityId, Long leadId, Long taskId,Long versionId) {
        Comments comment = new Comments();
        comment.setCommentsTitle(dto.getCommentsTitle());
        comment.setCommentDescription(dto.getCommentDescription());
        String userFirstNameLastName = RadiusUtil.getCurrentUsername();
        comment.setCreatedBy(userFirstNameLastName);
        comment.setCreatedOn(ZonedDateTime.now().toInstant().toEpochMilli());
        System.out.println("date"+ZonedDateTime.now().toInstant().toEpochMilli());
        if (campaignId != null) {
            Campaign campaign = campaignRepository.findById(campaignId)
                    .orElseThrow(() -> new IllegalArgumentException("Campaign not found with ID: " + campaignId));
            comment.setCampaign(campaign);
        }
        if (opportunityId != null) {
            Opportunity opportunity = opportunityRepository.findById(opportunityId)
                    .orElseThrow(() -> new IllegalArgumentException("Opportunity not found with ID: " + opportunityId));
            comment.setOpportunity(opportunity);
        }
        if (leadId != null) {
            Lead lead = leadRepository.findById(leadId)
                    .orElseThrow(() -> new IllegalArgumentException("Lead not found with ID: " + leadId));
            comment.setLead(lead);
        }
        if (taskId != null) {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));
            comment.setTask(task);
        }

        if (versionId != null) {
            Version version = versionRepository.findById(versionId)
                    .orElseThrow(() -> new IllegalArgumentException("Version not found with ID: " + versionId));
            comment.setVersion(version);
        }

        Comments saved = commentsRepository.save(comment);

        CommentsDTO response = new CommentsDTO();
        response.setCommentId(saved.getCommentId());
        response.setCommentsTitle(saved.getCommentsTitle());
        response.setCommentDescription(saved.getCommentDescription());
        response.setCreatedBy(saved.getCreatedBy());
        response.setCreatedOn(saved.getCreatedOn());
//        response.setUnreadFlag(saved.isUnreadFlag());
        if (saved.getCampaign() != null) {
            response.setCampaignId(saved.getCampaign().getCampaignId());
        }
        if (saved.getOpportunity() != null) {
            response.setOpportunityId(saved.getOpportunity().getOpportunityId());
        }
        if (saved.getLead() != null) {
            response.setLeadId(saved.getLead().getLeadId());
        }
        if (saved.getTask() != null) {
            response.setTaskId(saved.getTask().getTaskId());
        }
        if (saved.getVersion() != null) {
            response.setVersionId(saved.getVersion().getVersionId());
        }

        return response;
    }

    public List<CommentsDTO> getCommentsByCampaignId(Long campaignId)
    {
        List<CommentsDTO> commentsDTOList = new ArrayList<>();
        CampaignCommentTrack campaignCommentTrack = campaignCommentTrackService.getUserCampaignCommentTrack(getCurrentUsername(),campaignId);
        for(Comments comments: commentsRepository.findByCampaignCampaignIdOrderByCreatedOnDesc(campaignId)){

        commentsDTOList.add(transformToCommentsDTOWithFlag(comments, false));

        }
       return commentsDTOList;
    }

    public List<CommentsDTO> getCommentsByOpportunityId(Long opportunityId)
    {
        List<CommentsDTO> commentsDTOList = new ArrayList<>();
        OpportunityCommentTrack opportunityCommentTrack = opportunityCommentTrackService.getUserOpportunityCommentTrack(getCurrentUsername(),opportunityId);
        for(Comments comments : commentsRepository.findByOpportunityOpportunityIdOrderByCreatedOnDesc(opportunityId)){

            commentsDTOList.add(transformToCommentsDTOWithFlag(comments, false));
        }

        return commentsDTOList;
    }

    public List<CommentsDTO> getCommentsByLeadIdAndTasks(Long leadId) {
        List<CommentsDTO> commentsDTOList = new ArrayList<>();

        // Lead comments
        List<Comments> leadComments = commentsRepository.findByLeadLeadIdOrderByCreatedOnDesc(leadId);
        leadComments.forEach(comment -> {
            commentsDTOList.add(transformToCommentsDTOWithFlag(comment, false));
        });

        return commentsDTOList;
    }

    public List<CommentsDTO> getCommentsByVersionId(Long versionId) {
        List<CommentsDTO> commentsDTOList = new ArrayList<>();

        for (Comments comments : commentsRepository.findByVersion_VersionIdOrderByCreatedOnDesc(versionId)) {
            commentsDTOList.add(transformToCommentsDTOWithFlag(comments, false));
        }
        return commentsDTOList;
    }

    public List<CommentsDTO> getCommentsByTaskId(Long taskId)
    {
        List<CommentsDTO> commentsDTOList = new ArrayList<>();
        TaskCommentTrack taskCommentTrack = taskCommentTrackService.getUserTaskCommentTrack(getCurrentUsername(),taskId);

        for (Comments comments : commentsRepository.findByTaskTaskIdOrderByCreatedOnDesc(taskId)) {

            commentsDTOList.add(transformToCommentsDTOWithFlag(comments, false));
        }
        return commentsDTOList;
    }

    public Optional<Comments> getCommentsById(Long id)
    {
        return commentsRepository.findById(id);
    }

    public boolean deleteComments(Long id)
    {
        if(commentsRepository.existsById(id))
        {
            commentsRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private Comments transformToComments(CommentsDTO commentsDTO)
    {
        long currentTime = System.currentTimeMillis();
        System.out.println("Current Time in millis: " + currentTime);
        System.out.println("Readable Time: " + new java.util.Date(currentTime));
        Comments comments = new Comments();
        if(commentsDTO.getCommentId()!= null)
        {
            comments.setCommentId(comments.getCommentId());
        }
        comments.setCommentsTitle(commentsDTO.getCommentsTitle());
        comments.setCommentDescription(commentsDTO.getCommentDescription());
        comments.setCreatedBy(commentsDTO.getCreatedBy());

        Optional<Campaign>campaign = Optional.empty();
        Optional<Lead> lead = Optional.empty();
        Optional<Opportunity> opportunity = Optional.empty();
        Optional<Task> task = Optional.empty();
        if(commentsDTO.getCampaignId()!=null) {
            campaign = campaignRepository.findById(commentsDTO.getCampaignId());
        }
        if(commentsDTO.getLeadId() != null) {
            lead = leadRepository.findById(commentsDTO.getLeadId());
        }
        if(commentsDTO.getOpportunityId()!= null) {
            opportunity = opportunityRepository.findById(commentsDTO.getOpportunityId());
        }
        if(commentsDTO.getTaskId()!= null){
            task = taskRepository.findById(commentsDTO.getTaskId());
        }

        if(campaign.isPresent())
        {
            comments.setCampaign(campaign.get());
        }else if(lead.isPresent())
        {
            comments.setLead(lead.get());
        }else if(opportunity.isPresent())
        {
            comments.setOpportunity(opportunity.get());
        }
        else if(task.isPresent())
        {
            comments.setTask(task.get());
        }
        else {
            throw new IllegalArgumentException("could not find any id in between campaign, lead,Opportunity,task");
        }

        if(commentsDTO.getCreatedOn()== null) {
            comments.setCreatedOn(System.currentTimeMillis());

        }
        return comments;
    }

    private CommentsDTO transformToCommentsDTO(Comments comment) {
        CommentsDTO dto = new CommentsDTO();
        dto.setCommentId(comment.getCommentId());
        dto.setCommentsTitle(comment.getCommentsTitle());
        dto.setCommentDescription(comment.getCommentDescription());
        dto.setCreatedBy(comment.getCreatedBy());
        dto.setCreatedOn(comment.getCreatedOn());
        dto.setIsSystemComment(comment.getIsSystemComment());
        dto.setUnreadFlag(false); // Default false
        if (comment.getCampaign() != null) {
            dto.setCampaignId(comment.getCampaign().getCampaignId());
        }
        if (comment.getLead() != null) {
            dto.setLeadId(comment.getLead().getLeadId());
        }
        if (comment.getOpportunity() != null) {
            dto.setOpportunityId(comment.getOpportunity().getOpportunityId());
        }
        if (comment.getTask() != null) {
            dto.setTaskId(comment.getTask().getTaskId());
        }
        return dto;
    }

private CommentsDTO transformToCommentsDTOWithFlag(Comments comments, Boolean unreadFlag) {
    CommentsDTO commentsDTO = new CommentsDTO();
    commentsDTO.setCommentId(comments.getCommentId());
    commentsDTO.setCommentsTitle(comments.getCommentsTitle());
    commentsDTO.setCommentDescription(comments.getCommentDescription());
    commentsDTO.setCreatedBy(comments.getCreatedBy());
    commentsDTO.setUnreadFlag(unreadFlag);
    commentsDTO.setIsSystemComment(comments.getIsSystemComment());
    if (comments.getCampaign() != null) {
        commentsDTO.setCampaignId(comments.getCampaign().getCampaignId());
    }
    if (comments.getLead() != null) {
        commentsDTO.setLeadId(comments.getLead().getLeadId());
    }
    if (comments.getOpportunity() != null) {
        commentsDTO.setOpportunityId(comments.getOpportunity().getOpportunityId());
    }
    if (comments.getTask() != null) {
        commentsDTO.setTaskId(comments.getTask().getTaskId());
    }
    if(comments.getVersion() != null){
        commentsDTO.setVersionId(comments.getVersion().getVersionId());
    }
    commentsDTO.setCreatedOn(comments.getCreatedOn());
    return commentsDTO;
}

}
