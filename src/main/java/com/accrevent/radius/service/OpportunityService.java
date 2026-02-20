package com.accrevent.radius.service;

import com.accrevent.radius.dto.CampaignDTO;
import com.accrevent.radius.dto.LeadDTO;
import com.accrevent.radius.model.*;
import com.accrevent.radius.repository.*;
import com.accrevent.radius.dto.OpportunityDTO;
import com.accrevent.radius.util.LifecycleName;
import com.accrevent.radius.util.RadiusConstants;
import com.accrevent.radius.util.RadiusUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.accrevent.radius.util.RadiusUtil.*;

@Service
public class OpportunityService {
    private final OpportunityRepository opportunityRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ConstantLifecycleRepository constantLifecycleRepository;
    private final UserOpportunitySequenceRepository userOpportunitySequenceRepository;
    private final CommentsRepository commentsRepository;

    @Autowired
    private UserOpportunityViewRepository userOpportunityViewRepository;

    public OpportunityService(OpportunityRepository opportunityRepository,
                              WorkspaceRepository workspaceRepository,
                              ConstantLifecycleRepository constantLifecycleRepository,
                              UserOpportunitySequenceRepository userOpportunitySequenceRepository,
                              CommentsRepository commentsRepository)
    {
        this.opportunityRepository = opportunityRepository;
        this.workspaceRepository = workspaceRepository;
        this.constantLifecycleRepository = constantLifecycleRepository;
        this.userOpportunitySequenceRepository = userOpportunitySequenceRepository;
        this.commentsRepository = commentsRepository;
    }

    public Long getLastOpportunityViewed(String userName, Long opportunityId) {
        Optional<UserOpportunityView> viewRecordOpt = userOpportunityViewRepository.findByUserNameAndOpportunityId(userName, opportunityId);
        return viewRecordOpt.map(UserOpportunityView::getLastOpportunityViewed).orElse(null);
    }

    public OpportunityDTO createOpportunity(OpportunityDTO opportunityDTO)
    {
        opportunityDTO.setCreatedOn(null);
        Opportunity opportunity = transformToOpportunity(opportunityDTO);
        if(opportunity.getOpportunitylifecycleList().isEmpty()) {
            List<Lifecycle> lifecycleList = new ArrayList<>();
            List<ConstantLifecycle> constantLifecycles = constantLifecycleRepository.findByCycleId(3L);
            AtomicBoolean flag = new AtomicBoolean(true);
            constantLifecycles.forEach(constantLifecycle -> {
                Lifecycle lifecycle = new Lifecycle();
                lifecycle.setLifecycleName(constantLifecycle.getCycleName());
                lifecycle.setOpportunity(opportunity);
                lifecycle.setStatus("inActive");
                lifecycle.setType("DEFAULT");
                if(flag.get())
                {
                    lifecycle.setStatus("active");
                    flag.set(false);
                }

                lifecycleList.add(lifecycle);
            });
            opportunity.setOpportunitylifecycleList(lifecycleList);
        }
        //for opportunity system creation comment
        // Save opportunity first so it has ID and cascades lifecycles
        Opportunity savedOpportunity = opportunityRepository.save(opportunity);

        // System-generated comment for opportunity creation
        String username = RadiusUtil.getCurrentUsername();

        StringBuilder commentBuilder = new StringBuilder();
        commentBuilder.append(username)
                .append(" created the opportunity.");

        Comments comment = new Comments();
        comment.setOpportunity(savedOpportunity);
        comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
        comment.setCommentDescription(commentBuilder.toString());
        comment.setCreatedOn(System.currentTimeMillis());
        comment.setIsSystemComment(true);

        commentsRepository.save(comment);
        return transformToOpportunityDTO(opportunityRepository.save(opportunity));
    }


    public String buildPathForOpportunity(Long opportunityId) {
        return opportunityRepository.findById(opportunityId)
                .map(opportunity -> {
                    String workspaceName = opportunity.getWorkspace().getWorkspaceName();
                    String opportunityName = opportunity.getOpportunityName();
                    return workspaceName + " > " + opportunityName;
                })
                .orElse("Path not found");
    }




//    @Transactional
//    public OpportunityDTO updateOpportunity(OpportunityDTO opportunityDTO)
//    {
//        Optional<Opportunity> existingOpportunityOpt = opportunityRepository.findById(opportunityDTO.getOpportunityId());
//        if(!existingOpportunityOpt.isPresent())
//        {
//            throw new IllegalArgumentException("Opportunity does not exist");
//        }
//        Opportunity existingOpportunity = existingOpportunityOpt.get();
//
//        StringBuilder changes = new StringBuilder();
//
//        String username = RadiusUtil.getCurrentUsername();
//
//        // Compare name change
//        if(existingOpportunity.getOpportunityName() == null){
//            if(opportunityDTO.getOpportunityName()!=null){
//                changes.append(username)
//                        .append("' has updated the Opportunity name from '")
//                        .append(existingOpportunity.getOpportunityName())
//                        .append("' to '")
//                        .append(opportunityDTO.getOpportunityName()).append("'");
//            }
//        }else if(opportunityDTO.getOpportunityName()== null){
//            changes.append(username)
//                    .append("' has updated the Opportunity name from '")
//                    .append(existingOpportunity.getOpportunityName())
//                    .append("' to '")
//                    .append(opportunityDTO.getOpportunityName()).append("'");
//        }
//        else if (!existingOpportunity.getOpportunityName().equals(opportunityDTO.getOpportunityName())) {
//            changes.append(username)
//                    .append("' has updated the Opportunity name from '")
//                    .append(existingOpportunity.getOpportunityName())
//                    .append("' to '")
//                    .append(opportunityDTO.getOpportunityName()).append("'");
//        }
//
//        // Compare description change
//        if(existingOpportunity.getDescription()== null){
//            if(opportunityDTO.getDescription()!=null){
//                changes.append(username)
//                        .append("' has updated the description from '")
//                        .append(existingOpportunity.getDescription())
//                        .append("' to '")
//                        .append(opportunityDTO.getDescription()).append("'");
//            }
//        }else if(opportunityDTO.getDescription()== null){
//            changes.append(username)
//                    .append("' has updated the description from '")
//                    .append(existingOpportunity.getDescription())
//                    .append("' to '")
//                    .append(opportunityDTO.getDescription()).append("'");
//        }
//        else if (!existingOpportunity.getDescription().equals(opportunityDTO.getDescription())) {
//            changes.append(username)
//                    .append("' has updated the description from '")
//                    .append(existingOpportunity.getDescription())
//                    .append("' to '")
//                    .append(opportunityDTO.getDescription()).append("'");
//        }
//
//
//        if(!existingOpportunity.getWorkspace().getWorkspaceId().equals(opportunityDTO.getWorkspaceId()))
//        {
//            changes.append(username)
//                    .append("' has updated the Workspace from '")
//                    .append(existingOpportunity.getWorkspace().getWorkspaceId())
//                    .append("' to '")
//                    .append(opportunityDTO.getWorkspaceId()).append("'");
//        }
//
//        //Compare Requirement changes
//        if(existingOpportunity.getRequirement()== null){
//            if(opportunityDTO.getRequirement()!= null){
//                changes.append(username)
//                        .append("' has updated the Requirement from '")
//                        .append(existingOpportunity.getRequirement())
//                        .append("' to '")
//                        .append(opportunityDTO.getRequirement()).append("'");
//            }
//        }else if(opportunityDTO.getRequirement()== null){
//            changes.append(username)
//                    .append("' has updated the Requirement from '")
//                    .append(existingOpportunity.getRequirement())
//                    .append("' to '")
//                    .append(opportunityDTO.getRequirement()).append("'");
//        }
//        else if(!existingOpportunity.getRequirement().equals(opportunityDTO.getRequirement()))
//        {
//            changes.append(username)
//                    .append("' has updated the Requirement from '")
//                    .append(existingOpportunity.getRequirement())
//                    .append("' to '")
//                    .append(opportunityDTO.getRequirement()).append("'");
//
//        }
//
//        //Compare Customer change
//        if(existingOpportunity.getCustomer() == null){
//            if(opportunityDTO.getCustomer()!= null){
//                changes.append(username)
//                        .append("' has updated the Customer from '")
//                        .append(existingOpportunity.getCustomer())
//                        .append("' to '")
//                        .append(opportunityDTO.getCustomer()).append("'");
//            }
//        }else if(opportunityDTO.getCustomer()== null){
//            changes.append(username)
//                    .append("' has updated the Customer from '")
//                    .append(existingOpportunity.getCustomer())
//                    .append("' to '")
//                    .append(opportunityDTO.getCustomer()).append("'");
//        }
//        else if(!existingOpportunity.getCustomer().equals(opportunityDTO.getCustomer()))
//        {
//            changes.append(username)
//                    .append("' has updated the Customer from '")
//                    .append(existingOpportunity.getCustomer())
//                    .append("' to '")
//                    .append(opportunityDTO.getCustomer()).append("'");
//
//        }
//
//        //Compare EstimateRevenue change
//        if(existingOpportunity.getEstimateRevenue()== null){
//            if(opportunityDTO.getEstimateRevenue()!=null){
//                changes.append(username)
//                        .append("' has updated the Estimate Revenue from '")
//                        .append(existingOpportunity.getEstimateRevenue())
//                        .append("' to '")
//                        .append(opportunityDTO.getEstimateRevenue()).append("'");
//            }
//        }else if(opportunityDTO.getEstimateRevenue()==null){
//            changes.append(username)
//                    .append("' has updated the Estimate Revenue from '")
//                    .append(existingOpportunity.getEstimateRevenue())
//                    .append("' to '")
//                    .append(opportunityDTO.getEstimateRevenue()).append("'");
//        }
//        else if(!existingOpportunity.getEstimateRevenue().equals(opportunityDTO.getEstimateRevenue()))
//        {
//            changes.append(username)
//                    .append("' has updated the Estimate Revenue from '")
//                    .append(existingOpportunity.getEstimateRevenue())
//                    .append("' to '")
//                    .append(opportunityDTO.getEstimateRevenue()).append("'");
//
//        }
//
//        //Compare Currency change
//        if(existingOpportunity.getCurrency()== null){
//            if(opportunityDTO.getCurrency()!=null){
//                changes.append(username)
//                        .append("' has updated the Currency from '")
//                        .append(existingOpportunity.getCurrency())
//                        .append("' to '")
//                        .append(opportunityDTO.getCurrency()).append("'");
//            }
//        }else if(opportunityDTO.getCurrency()==null){
//            changes.append(username)
//                    .append("' has updated the Currency from '")
//                    .append(existingOpportunity.getCurrency())
//                    .append("' to '")
//                    .append(opportunityDTO.getCurrency()).append("'");
//        }
//        else if(!existingOpportunity.getCurrency().equals(opportunityDTO.getCurrency()))
//        {
//            changes.append(username)
//                    .append("' has updated the Currency from '")
//                    .append(existingOpportunity.getCurrency())
//                    .append("' to '")
//                    .append(opportunityDTO.getCurrency()).append("'");
//
//        }
//
//        //Compare ProjectTitle change
//        if(existingOpportunity.getProjectTitle()==null){
//            if(opportunityDTO.getProjectTitle()!= null){
//                changes.append(username)
//                        .append("' has updated the Project Title from '")
//                        .append(existingOpportunity.getProjectTitle())
//                        .append("' to '")
//                        .append(opportunityDTO.getProjectTitle()).append("'");
//            }
//        }else if(opportunityDTO.getProjectTitle()==null){
//            changes.append(username)
//                    .append("' has updated the Project Title from '")
//                    .append(existingOpportunity.getProjectTitle())
//                    .append("' to '")
//                    .append(opportunityDTO.getProjectTitle()).append("'");
//        }
//        else if(!existingOpportunity.getProjectTitle().equals(opportunityDTO.getProjectTitle()))
//        {
//            changes.append(username)
//                    .append("' has updated the Project Title from '")
//                    .append(existingOpportunity.getProjectTitle())
//                    .append("' to '")
//                    .append(opportunityDTO.getProjectTitle()).append("'");
//
//        }
//
//        //Compare BusinessUnit change
//        if(existingOpportunity.getBusinessUnit()== null){
//            if(opportunityDTO.getBusinessUnit()!=null){
//                changes.append(username)
//                        .append("' has updated the Business Unit from '")
//                        .append(existingOpportunity.getBusinessUnit())
//                        .append("' to '")
//                        .append(opportunityDTO.getBusinessUnit()).append("'");
//            }
//        }else if(opportunityDTO.getBusinessUnit()==null){
//            changes.append(username)
//                    .append("' has updated the Business Unit from '")
//                    .append(existingOpportunity.getBusinessUnit())
//                    .append("' to '")
//                    .append(opportunityDTO.getBusinessUnit()).append("'");
//        }
//        else if(!existingOpportunity.getBusinessUnit().equals(opportunityDTO.getBusinessUnit()))
//        {
//            changes.append(username)
//                    .append("' has updated the Business Unit from '")
//                    .append(existingOpportunity.getBusinessUnit())
//                    .append("' to '")
//                    .append(opportunityDTO.getBusinessUnit()).append("'");
//        }
//
//
//        Opportunity opportunity = transformToOpportunity(opportunityDTO);
//
//
//        Boolean isSystemComment = false;
//
//
//
//        if (changes.length() > 0) {
//            Comments comment = new Comments();
//            comment.setOpportunity(opportunity);
////          comment.setCreatedBy(username);
//            comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
//            comment.setCommentDescription(changes.toString());
//            comment.setCreatedOn(System.currentTimeMillis());
//            comment.setIsSystemComment(true);
//            commentsRepository.save(comment);
//
//            isSystemComment = true;
//
//
//        }
//
//
//        Opportunity savedOpportunity  = opportunityRepository.save(opportunity);
//
//        OpportunityDTO updatedDTO = transformToOpportunityDTO(savedOpportunity);
//
//        updatedDTO.setIsSystemComment(isSystemComment);
//        System.out.println("isSystemComment"+isSystemComment);
//        return updatedDTO;
//    }


@Transactional
public OpportunityDTO updateOpportunity(OpportunityDTO opportunityDTO)
{
    Optional<Opportunity> existingOpportunityOpt = opportunityRepository.findById(opportunityDTO.getOpportunityId());
    if(!existingOpportunityOpt.isPresent())
    {
        throw new IllegalArgumentException("Opportunity does not exist");
    }
    Opportunity existingOpportunity = existingOpportunityOpt.get();

    StringBuilder changes = new StringBuilder();

    String username = RadiusUtil.getCurrentUsername();

    //Archeive Comment
    if (opportunityDTO.getIsArchived() != null &&
            !opportunityDTO.getIsArchived().equals(existingOpportunity.getIsArchived())) {

        changes.append(username)
                .append(" ")
                .append(opportunityDTO.getIsArchived() ? "archived" : "unarchived")
                .append(" this Opportunity.");
    }

    // Compare name change
    if(existingOpportunity.getOpportunityName() == null){
        if(opportunityDTO.getOpportunityName()!=null){
            changes.append(username)
                    .append(" updated the Title. Old Value: '")
                    .append(existingOpportunity.getOpportunityName())
                    .append("'  New Value: '")
                    .append(opportunityDTO.getOpportunityName()).append("'");
        }
    }else if(opportunityDTO.getOpportunityName()== null){
        changes.append(username)
                .append(" updated the Title. Old Value: '")
                .append(existingOpportunity.getOpportunityName())
                .append("'   New Value: '")
                .append(opportunityDTO.getOpportunityName()).append("'");
    }
    else if (!existingOpportunity.getOpportunityName().equals(opportunityDTO.getOpportunityName())) {
        changes.append(username)
                .append(" updated the Title. Old Value: '")
                .append(existingOpportunity.getOpportunityName())
                .append("'   New Value: '")
                .append(opportunityDTO.getOpportunityName()).append("'");
    }

    // Compare description change-my first
//        String oldDescription = existingOpportunity.getDescription();
//        String newDescription = opportunityDTO.getDescription();
//
//        boolean isOldEmpty = (oldDescription == null || oldDescription.trim().isEmpty() || oldDescription.equalsIgnoreCase("N/A"));
//        boolean isNewEmpty = (newDescription == null || newDescription.trim().isEmpty());
//
//        if (isOldEmpty && !isNewEmpty) {
//            changes.append(username)
//                    .append(" added a Description: '")
//                    .append(newDescription)
//                    .append("'. ");
//        }
//        else if (!isOldEmpty && !isNewEmpty && !oldDescription.equals(newDescription)) {
//            changes.append(username)
//                    .append(" updated the Description.\n")
//                    .append("Old Value: '")
//                    .append(oldDescription)
//                    .append("'\n")
//                    .append("New Value: '")
//                    .append(newDescription)
//                    .append("'. ");
//        }

    // --- Description ---
    String oldDescription = existingOpportunity.getDescription();
    String newDescription = opportunityDTO.getDescription();

    boolean isOldEmpty = oldDescription == null || oldDescription.trim().isEmpty() || oldDescription.equalsIgnoreCase("N/A");
    boolean isNewEmpty = newDescription == null || newDescription.trim().isEmpty() || newDescription.equalsIgnoreCase("N/A");

    if (isOldEmpty && !isNewEmpty) {
        changes.append(username)
                .append(" added a Description: '")
                .append(newDescription)
                .append("'. ");
    } else if (!isOldEmpty && isNewEmpty) {
        changes.append(username)
                .append(" removed the Description. Previous Value: '")
                .append(oldDescription)
                .append("'. ");
    } else if (!isOldEmpty && !isNewEmpty && !oldDescription.equals(newDescription)) {
        changes.append(username)
                .append(" updated the Description.\n")
                .append("Old Value: '")
                .append(oldDescription)
                .append("'\n")
                .append("New Value: '")
                .append(newDescription)
                .append("'. ");
    }


    // ------------------ Compare Description ------------------
//        String oldDescription = existingOpportunity.getDescription();
//        String newDescription = opportunityDTO.getDescription();
//
//        boolean isOldEmpty = (oldDescription == null || oldDescription.trim().isEmpty() || oldDescription.equalsIgnoreCase("N/A"));
//        boolean isNewEmpty = (newDescription == null || newDescription.trim().isEmpty());
//
//        if (isOldEmpty && !isNewEmpty) {
//            changes.append(username)
//                    .append(" added a Description: '")
//                    .append(newDescription)
//                    .append("'. ");
//        } else if (!isOldEmpty && isNewEmpty) {
//            changes.append(username)
//                    .append(" removed the Description. Previous Value: '")
//                    .append(oldDescription)
//                    .append("'. ");
//        } else if (!isOldEmpty && !isNewEmpty && !oldDescription.equals(newDescription)) {
//            changes.append(username)
//                    .append(" updated the Description.\n")
//                    .append("Old Value: '")
//                    .append(oldDescription)
//                    .append("'\n")
//                    .append("New Value: '")
//                    .append(newDescription)
//                    .append("'. ");
//        }




    if(!existingOpportunity.getWorkspace().getWorkspaceId().equals(opportunityDTO.getWorkspaceId()))
    {
        changes.append(username)
                .append(" has updated the Workspace from '")
                .append(existingOpportunity.getWorkspace().getWorkspaceId())
                .append("' to '")
                .append(opportunityDTO.getWorkspaceId()).append("'");
    }

    //Compare Requirement changes
    String oldRequirement = existingOpportunity.getRequirement();
    String newRequirement = opportunityDTO.getRequirement();

    isOldEmpty = (oldRequirement == null || oldRequirement.trim().isEmpty() || oldRequirement.equalsIgnoreCase("N/A"));
    isNewEmpty = (newRequirement == null || newRequirement.trim().isEmpty() || newRequirement.equalsIgnoreCase("N/A"));

    if (isOldEmpty && !isNewEmpty) {
        changes.append(username)
                .append(" added a Requirement: '")
                .append(newRequirement)
                .append("'. ");
    } else if (!isOldEmpty && isNewEmpty) {
        changes.append(username)
                .append(" removed the Requirement. Previous Value: '")
                .append(oldRequirement)
                .append("'. ");
    } else if (!isOldEmpty && !isNewEmpty && !oldRequirement.equals(newRequirement)) {
        changes.append(username)
                .append(" updated the Requirement.\n")
                .append("Old Value: '")
                .append(oldRequirement)
                .append("'\n")
                .append("New Value: '")
                .append(newRequirement)
                .append("'. ");
    }

    // ------------------ Compare Requirement ------------------
//        String oldRequirement = existingOpportunity.getRequirement();
//        String newRequirement = opportunityDTO.getRequirement();
//
//        isOldEmpty = (oldRequirement == null || oldRequirement.trim().isEmpty() || oldRequirement.equalsIgnoreCase("N/A"));
//        isNewEmpty = (newRequirement == null || newRequirement.trim().isEmpty());
//
//        if (isOldEmpty && !isNewEmpty) {
//            changes.append(username)
//                    .append(" added a Requirement: '")
//                    .append(newRequirement)
//                    .append("'. ");
//        } else if (!isOldEmpty && isNewEmpty) {
//            changes.append(username)
//                    .append(" removed the Requirement. Previous Value: '")
//                    .append(oldRequirement)
//                    .append("'. ");
//        } else if (!isOldEmpty && !isNewEmpty && !oldRequirement.equals(newRequirement)) {
//            changes.append(username)
//                    .append(" updated the Requirement.\n")
//                    .append("Old Value: '")
//                    .append(oldRequirement)
//                    .append("'\n")
//                    .append("New Value: '")
//                    .append(newRequirement)
//                    .append("'. ");
//        }


// ------------------ Compare Customer ------------------
//        String oldCustomer = existingOpportunity.getCustomer();
//        String newCustomer = opportunityDTO.getCustomer();
//
//        isOldEmpty = (oldCustomer == null || oldCustomer.trim().isEmpty() || oldCustomer.equalsIgnoreCase("N/A"));
//        isNewEmpty = (newCustomer == null || newCustomer.trim().isEmpty());
//
//        if (isOldEmpty && !isNewEmpty) {
//            changes.append(username)
//                    .append(" added a Customer: '")
//                    .append(newCustomer)
//                    .append("'. ");
//        } else if (!isOldEmpty && isNewEmpty) {
//            changes.append(username)
//                    .append(" removed the Customer. Previous Value: '")
//                    .append(oldCustomer)
//                    .append("'. ");
//        } else if (!isOldEmpty && !isNewEmpty && !oldCustomer.equals(newCustomer)) {
//            changes.append(username)
//                    .append(" updated the Customer.\n")
//                    .append("Old Value: '")
//                    .append(oldCustomer)
//                    .append("'\n")
//                    .append("New Value: '")
//                    .append(newCustomer)
//                    .append("'. ");
//        }

//        //Compare Customer change
    String oldCustomer = existingOpportunity.getCustomer();
    String newCustomer = opportunityDTO.getCustomer();

    isOldEmpty = (oldCustomer == null || oldCustomer.trim().isEmpty() || oldCustomer.equalsIgnoreCase("N/A"));
    isNewEmpty = (newCustomer == null || newCustomer.trim().isEmpty() || newCustomer.equalsIgnoreCase("N/A"));


    if (isOldEmpty && !isNewEmpty) {
        changes.append(username)
                .append(" added a Customer: '")
                .append(newCustomer)
                .append("'. ");
    } else if (!isOldEmpty && isNewEmpty) {
        changes.append(username)
                .append(" removed the Customer. Previous Value: '")
                .append(oldCustomer)
                .append("'. ");
    } else if (!isOldEmpty && !isNewEmpty && !oldCustomer.equals(newCustomer)) {
        changes.append(username)
                .append(" updated the Customer.\n")
                .append("Old Value: '")
                .append(oldCustomer)
                .append("'\n")
                .append("New Value: '")
                .append(newCustomer)
                .append("'. ");
    }
//
//
//        // Fetch old and new values
//        Double oldRevenue = Double.valueOf(existingOpportunity.getEstimateRevenue());
//        Double newRevenue = Double.valueOf(opportunityDTO.getEstimateRevenue());
//
//        String oldCurrency = existingOpportunity.getCurrency();
//        String newCurrency = opportunityDTO.getCurrency();
//
//// Prepare readable combined strings
//        String oldValue = (oldCurrency != null ? oldCurrency : "") + " " + (oldRevenue != null ? oldRevenue : "");
//        String newValue = (newCurrency != null ? newCurrency : "") + " " + (newRevenue != null ? newRevenue : "");
//
//// Check if any change happened
//        boolean revenueChanged = (oldRevenue == null && newRevenue != null) ||
//                (oldRevenue != null && newRevenue == null) ||
//                (oldRevenue != null && newRevenue != null && !oldRevenue.equals(newRevenue));
//
//        boolean currencyChanged = (oldCurrency == null && newCurrency != null) ||
//                (oldCurrency != null && newCurrency == null) ||
//                (oldCurrency != null && newCurrency != null && !oldCurrency.equals(newCurrency));
//
//        if (revenueChanged || currencyChanged) {
//            if (oldRevenue == null && oldCurrency == null) {
//                // Newly added
//                changes.append(username)
//                        .append(" added Estimated Revenue / Currency: ")
//                        .append(newValue.trim())
//                        .append(". ");
//            } else {
//                // Updated
//                changes.append(username)
//                        .append(" updated the Estimated Revenue / Currency.\n")
//                        .append("Old Value: ")
//                        .append(oldValue.trim())
//                        .append("\nNew Value: ")
//                        .append(newValue.trim())
//                        .append(". ");
//            }
//        }

    // Fetch old and new values
    Double oldRevenue = existingOpportunity.getEstimateRevenue() != null
            ? Double.valueOf(existingOpportunity.getEstimateRevenue()) : null;
    Double newRevenue = opportunityDTO.getEstimateRevenue() != null
            ? Double.valueOf(opportunityDTO.getEstimateRevenue()) : null;

    String oldCurrency = existingOpportunity.getCurrency();
    String newCurrency = opportunityDTO.getCurrency();

// Prepare readable combined strings
    String oldValue = (oldCurrency != null ? oldCurrency : "") + " " + (oldRevenue != null ? oldRevenue : "");
    String newValue = (newCurrency != null ? newCurrency : "") + " " + (newRevenue != null ? newRevenue : "");

// Check if any change happened
    boolean revenueChanged = (oldRevenue == null && newRevenue != null) ||
            (oldRevenue != null && newRevenue == null) ||
            (oldRevenue != null && newRevenue != null && !oldRevenue.equals(newRevenue));

    boolean currencyChanged = (oldCurrency == null && newCurrency != null) ||
            (oldCurrency != null && newCurrency == null) ||
            (oldCurrency != null && newCurrency != null && !oldCurrency.equals(newCurrency));

    boolean isOldRevenueEmpty = (oldRevenue == null || oldRevenue == 0.0);
    boolean isOldCurrencyEmpty = (oldCurrency == null || oldCurrency.trim().isEmpty() || oldCurrency.equalsIgnoreCase("N/A"));

    if (revenueChanged || currencyChanged) {
        if (isOldRevenueEmpty && isOldCurrencyEmpty) {
            // Newly added
            changes.append(username)
                    .append(" added Estimated Revenue / Currency: ")
                    .append(newValue.trim())
                    .append(". ");
        } else {
            // Updated
            changes.append(username)
                    .append(" updated the Estimated Revenue / Currency.\n")
                    .append("Old Value: ")
                    .append(oldValue.trim())
                    .append("\nNew Value: ")
                    .append(newValue.trim())
                    .append(". ");
        }
    }





//        //Compare EstimateRevenue change
//        if(existingOpportunity.getEstimateRevenue()== null){
//            if(opportunityDTO.getEstimateRevenue()!=null){
//                changes.append(username)
//                        .append("' updated the Estimated Revenue. Old Value:'")
//                        .append(existingOpportunity.getEstimateRevenue())
//                        .append("' New Value: '")
//                        .append(opportunityDTO.getEstimateRevenue()).append("'");
//            }
//        }else if(opportunityDTO.getEstimateRevenue()==null){
//            changes.append(username)
//                    .append("' updated the Estimated Revenue. Old Value: '")
//                    .append(existingOpportunity.getEstimateRevenue())
//                    .append("' New Value: '")
//                    .append(opportunityDTO.getEstimateRevenue()).append("'");
//        }
//        else if(!existingOpportunity.getEstimateRevenue().equals(opportunityDTO.getEstimateRevenue()))
//        {
//            changes.append(username)
//                    .append("' updated the Estimated Revenue. Old Value: '")
//                    .append(existingOpportunity.getEstimateRevenue())
//                    .append("' New Value: '")
//                    .append(opportunityDTO.getEstimateRevenue()).append("'");
//
//        }
//
//        //Compare Currency change
//        if(existingOpportunity.getCurrency()== null){
//            if(opportunityDTO.getCurrency()!=null){
//                changes.append(username)
//                        .append("' updated the Currency. Old Value: '")
//                        .append(existingOpportunity.getCurrency())
//                        .append("'  New Value: '")
//                        .append(opportunityDTO.getCurrency()).append("'");
//            }
//        }else if(opportunityDTO.getCurrency()==null){
//            changes.append(username)
//                    .append("'updated the Currency. Old Value: '")
//                    .append(existingOpportunity.getCurrency())
//                    .append("'  New Value: '")
//                    .append(opportunityDTO.getCurrency()).append("'");
//        }
//        else if(!existingOpportunity.getCurrency().equals(opportunityDTO.getCurrency()))
//        {
//            changes.append(username)
//                    .append("' updated the Currency. Old Value: '")
//                    .append(existingOpportunity.getCurrency())
//                    .append("'  New Value: '")
//                    .append(opportunityDTO.getCurrency()).append("'");
//
//        }








//        //Compare ProjectTitle change
//        String oldProjectTitle = existingOpportunity.getProjectTitle();
//        String newProjectTitle = opportunityDTO.getProjectTitle();
//
//         isOldEmpty = (oldProjectTitle == null || oldProjectTitle.trim().isEmpty() || oldProjectTitle.equalsIgnoreCase("N/A"));
//         isNewEmpty = (newProjectTitle == null || newProjectTitle.trim().isEmpty());
//
//        if (isOldEmpty && !isNewEmpty) {
//            changes.append(username)
//                    .append(" added a Project Title: '")
//                    .append(newProjectTitle)
//                    .append("'. ");
//        } else if (!isOldEmpty && isNewEmpty) {
//            changes.append(username)
//                    .append(" removed the Project Title. Previous Value: '")
//                    .append(oldProjectTitle)
//                    .append("'. ");
//        } else if (!isOldEmpty && !isNewEmpty && !oldProjectTitle.equals(newProjectTitle)) {
//            changes.append(username)
//                    .append(" updated the Project Title.\n")
//                    .append("Old Value: '")
//                    .append(oldProjectTitle)
//                    .append("'\n")
//                    .append("New Value: '")
//                    .append(newProjectTitle)
//                    .append("'. ");
//        }

    // ------------------ Compare Project Title ------------------
    String oldProjectTitle = existingOpportunity.getProjectTitle();
    String newProjectTitle = opportunityDTO.getProjectTitle();

    isOldEmpty = (oldProjectTitle == null || oldProjectTitle.trim().isEmpty() || oldProjectTitle.equalsIgnoreCase("N/A"));
    isNewEmpty = (newProjectTitle == null || newProjectTitle.trim().isEmpty()  || newProjectTitle.equalsIgnoreCase("N/A"));

    if (isOldEmpty && !isNewEmpty) {
        changes.append(username)
                .append(" added a Project Title: '")
                .append(newProjectTitle)
                .append("'. ");
    } else if (!isOldEmpty && isNewEmpty) {
        changes.append(username)
                .append(" removed the Project Title. Previous Value: '")
                .append(oldProjectTitle)
                .append("'. ");
    } else if (!isOldEmpty && !isNewEmpty && !oldProjectTitle.equals(newProjectTitle)) {
        changes.append(username)
                .append(" updated the Project Title.\n")
                .append("Old Value: '")
                .append(oldProjectTitle)
                .append("'\n")
                .append("New Value: '")
                .append(newProjectTitle)
                .append("'. ");
    }


    //Compare BusinessUnit change
    if(existingOpportunity.getBusinessUnit()== null){
        if(opportunityDTO.getBusinessUnit()!=null){
            changes.append(username)
                    .append(" updated the Business Unit. Old Value:'")
                    .append(existingOpportunity.getBusinessUnit())
                    .append("'  New Value: '")
                    .append(opportunityDTO.getBusinessUnit()).append("'");
        }
    }else if(opportunityDTO.getBusinessUnit()==null){
        changes.append(username)
                .append(" updated the Business Unit. Old Value: '")
                .append(existingOpportunity.getBusinessUnit())
                .append("'  New Value: '")
                .append(opportunityDTO.getBusinessUnit()).append("'");
    }
    else if(!existingOpportunity.getBusinessUnit().equals(opportunityDTO.getBusinessUnit()))
    {
        changes.append(username)
                .append(" updated the Business Unit. Old Value: '")
                .append(existingOpportunity.getBusinessUnit())
                .append("'  New Value: '")
                .append(opportunityDTO.getBusinessUnit()).append("'");
    }




    Opportunity opportunity = transformToOpportunity(opportunityDTO);


    Boolean isSystemComment = false;



    if (changes.length() > 0) {
        Comments comment = new Comments();
        comment.setOpportunity(opportunity);
        comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
        comment.setCommentDescription(changes.toString());
        comment.setCreatedOn(System.currentTimeMillis());
        comment.setIsSystemComment(true);
        commentsRepository.save(comment);

        isSystemComment = true;


    }


    Opportunity savedOpportunity  = opportunityRepository.save(opportunity);

    OpportunityDTO updatedDTO = transformToOpportunityDTO(savedOpportunity);

    updatedDTO.setIsSystemComment(isSystemComment);
    System.out.println("isSystemComment"+isSystemComment);
    return updatedDTO;
}


    @Transactional
    public void saveLastReadForOpportunity(String userName, Long opportunityId, Long lastOpportunityViewed) {

        // First, check if the opportunity exists
        Optional<Opportunity> opportunityOptional = opportunityRepository.findById(opportunityId);
        if (!opportunityOptional.isPresent()) {
            throw new IllegalArgumentException("Opportunity not found for ID: " + opportunityId);
        }

        // Check if an entry already exists
        Optional<UserOpportunityView> existing =
                userOpportunityViewRepository.findByUserNameAndOpportunityId(userName, opportunityId);

        UserOpportunityView entry;
        if (existing.isPresent()) {
            entry = existing.get();
        } else {
            entry = new UserOpportunityView();
            entry.setUserName(userName);
            entry.setOpportunityId(opportunityId);
        }

        // Update last viewed timestamp
        entry.setLastOpportunityViewed(lastOpportunityViewed);
        userOpportunityViewRepository.save(entry);
    }

    public List<Opportunity> getAllOpportunity()
    {
        return opportunityRepository.findAll();
    }

    public Optional<Opportunity> getOpportunityById(Long id)
    {
        return opportunityRepository.findById(id);
    }

    public boolean deleteOpportunity(Long id)
    {
        if(opportunityRepository.existsById(id))
        {
            opportunityRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private Opportunity transformToOpportunity(OpportunityDTO opportunityDTO)
    {
        Optional<Workspace>workspace = workspaceRepository.findById(opportunityDTO.getWorkspaceId());
        if(workspace.isPresent())
        {
            Opportunity opportunity;
            if(opportunityDTO.getOpportunityId()!= null)
            {
                Optional<Opportunity>opportunityOptional=opportunityRepository.findById(opportunityDTO.getOpportunityId());
                if(opportunityOptional.isPresent())
                {
                    opportunity = opportunityOptional.get();
                }else {
                    opportunity = new Opportunity();
                }
                opportunity.setOpportunityId(opportunity.getOpportunityId());
            }else {
                opportunity = new Opportunity();
            }
            opportunity.setOpportunityName(opportunityDTO.getOpportunityName());
            opportunity.setDescription(opportunityDTO.getDescription());
            opportunity.setWorkspace(workspace.get());
            opportunity.setRequirement(opportunityDTO.getRequirement());
            opportunity.setCustomer(opportunityDTO.getCustomer());
            opportunity.setEstimateRevenue(opportunityDTO.getEstimateRevenue());
            opportunity.setCurrency(opportunityDTO.getCurrency());
            opportunity.setProjectTitle(opportunityDTO.getProjectTitle());
            opportunity.setBusinessUnit(opportunityDTO.getBusinessUnit());
            opportunity.setIsArchived(opportunityDTO.getIsArchived() != null ?
                    opportunityDTO.getIsArchived() : false);

            // Defensive check: default to false if null
            opportunity.setIsSystemComment(opportunityDTO.getIsSystemComment() != null && opportunityDTO.getIsSystemComment());

            if(opportunityDTO.getCreatedOn() == null)
            {
                long currentTime = System.currentTimeMillis();
                System.out.println("Current Time in millis: " + currentTime);
                System.out.println("Readable Time: " + new java.util.Date(currentTime));
                opportunity.setCreatedOn(System.currentTimeMillis());
            }
        return opportunity;
        }
        else {
            throw new IllegalArgumentException("Workspace ID not found");
        }
    }
    private OpportunityDTO transformToOpportunityDTO(Opportunity opportunity)
    {
        OpportunityDTO opportunityDTO = new OpportunityDTO();
        opportunityDTO.setOpportunityId(opportunity.getOpportunityId());
        opportunityDTO.setOpportunityName(opportunity.getOpportunityName());
        opportunityDTO.setDescription(opportunity.getDescription());
        opportunityDTO.setWorkspaceId(opportunity.getWorkspace().getWorkspaceId());
        opportunityDTO.setRequirement(opportunity.getRequirement());
        opportunityDTO.setCustomer(opportunity.getCustomer());
        opportunityDTO.setEstimateRevenue(opportunity.getEstimateRevenue());
        opportunityDTO.setCurrency(opportunity.getCurrency());
        opportunityDTO.setProjectTitle(opportunity.getProjectTitle());
        opportunityDTO.setBusinessUnit(opportunity.getBusinessUnit());
        // Add archive status to DTO
        opportunityDTO.setIsArchived(opportunity.getIsArchived());
        if(opportunity.getCreatedOn() != null)
        {
            opportunityDTO.setCreatedOn(opportunity.getCreatedOn());
        }

        // Build path: WorkspaceName > LifecycleName
        String path = "";
        if (opportunity.getWorkspace() != null) {
            path = opportunity.getWorkspace().getWorkspaceName();
        }

        // Opportunity has a list of lifecycles mapped
        if (opportunity.getOpportunitylifecycleList() != null && !opportunity.getOpportunitylifecycleList().isEmpty()) {
            Lifecycle activeLifecycle = opportunity.getOpportunitylifecycleList().stream()
                    .filter(lc -> "active".equalsIgnoreCase(lc.getStatus()))
                    .findFirst()
                    .orElse(null);
            if (activeLifecycle != null) {
                path += " > " + activeLifecycle.getLifecycleName();
            }
        }

        opportunityDTO.setPath(path);

        return opportunityDTO;
    }

    public List<UserOpportunitySequence> updateOpportunitySequence(String userName, List<OpportunityDTO> orderedOpportunityDTOList)
    {
        int leadLength = orderedOpportunityDTOList.size();
        List<UserOpportunitySequence> sequencesToSave = new ArrayList<>();

        List<Long> opportunityIds = orderedOpportunityDTOList.stream()
                .map(OpportunityDTO::getOpportunityId).toList();

        List<UserOpportunitySequence> existingSequences = userOpportunitySequenceRepository.findByUserNameAndOpportunity_OpportunityIdIn(userName, opportunityIds);

        Map<Long, UserOpportunitySequence> sequenceMap = existingSequences.stream()
                .collect(Collectors.toMap(seq -> seq.getOpportunity().getOpportunityId(), seq -> seq));

        for(int i = 0; i < leadLength ; i++)
        {
            Long opportunityId = orderedOpportunityDTOList.get(i).getOpportunityId();
            int newSequenceOrder = i + 1;

            UserOpportunitySequence userOpportunitySequence = sequenceMap.get(opportunityId);

            if (userOpportunitySequence != null) {
                // Update the sequence order if needed
                if (userOpportunitySequence.getSequenceOrder() != newSequenceOrder) {
                    userOpportunitySequence.setSequenceOrder(newSequenceOrder);
                    sequencesToSave.add(userOpportunitySequence);
                }
            } else {
                // If no sequence exists, create a new one
                Optional<Opportunity> opportunity = opportunityRepository.findById(opportunityId);
                if (opportunity.isPresent()) {
                    UserOpportunitySequence newLeadSequence = new UserOpportunitySequence();
                    newLeadSequence.setOpportunity(opportunity.get());
                    newLeadSequence.setSequenceOrder(newSequenceOrder);
                    newLeadSequence.setUserName(userName);
                    sequencesToSave.add(newLeadSequence);
                }
            }
        }
        if (!sequencesToSave.isEmpty()) {
            userOpportunitySequenceRepository.saveAll(sequencesToSave);
        }
        return sequencesToSave;
    }

    public List<OpportunityDTO> getOpportunityByWorkspaceId(Long workspaceId)
    {
        List<OpportunityDTO>opportunityDTOList = new ArrayList<>();
        List<Opportunity> opportunityList = opportunityRepository.findByWorkspace_WorkspaceId(workspaceId);
        List<UserOpportunitySequence> userTaskSequences = userOpportunitySequenceRepository.findByUserName(getCurrentUsername());

        Map<Long, Integer> userSpecificSequences = userTaskSequences.stream()
                .collect(Collectors.toMap(
                        seq -> seq.getOpportunity().getOpportunityId(),
                        UserOpportunitySequence::getSequenceOrder
                ));

        opportunityList = opportunityList.stream()
                .sorted((c1, c2) -> {
                    // Get the user-specific sequence or default if not found
                    int seq1 = userSpecificSequences.getOrDefault(c1.getOpportunityId(), getDefaultSequenceOrder());
                    int seq2 = userSpecificSequences.getOrDefault(c2.getOpportunityId(), getDefaultSequenceOrder());
                    // Compare the two sequence orders
                    return Integer.compare(seq1, seq2);
                }).toList();
        opportunityList.forEach(opportunity ->
            opportunityDTOList.add(transformToOpportunityDTO(opportunity))
        );
        return opportunityDTOList;
    }


    public int getDefaultSequenceOrder() {
        return 999; // Example of default sequence order, change as per logic
    }

public List<OpportunityDTO> getOpportunitiesWithTasksInRange(
        String assignTo,
        long startDate,
        long endDate,
        int todayYear,
        int todayMonth,
        int todayDay) {

    long today = getStartOfDayGMT(todayYear, todayMonth, todayDay);
    List<Opportunity> allOpportunities = opportunityRepository.findAll(); // fetch all opportunities
    List<OpportunityDTO> result = new ArrayList<>();

    for (Opportunity opportunity : allOpportunities) {

        boolean hasTaskInRange = false;

        // ----- 1. Check opportunity lifecycle tasks + subtasks -----
        for (Lifecycle lc : opportunity.getOpportunitylifecycleList()) {
            hasTaskInRange = lc.getTaskList().stream()
                    .anyMatch(t -> isTaskAssignedAndInRange(t, assignTo, startDate, endDate, today));
            if (hasTaskInRange) break;
        }

        // ----- 2. Add opportunity if at least one task in range -----
        if (hasTaskInRange) {
            OpportunityDTO dto = mapOpportunityToDTO(opportunity);
            result.add(dto);
        }
    }

    return result;
}

    /**
     * Checks task and its subtasks recursively
     */
    private boolean isTaskAssignedAndInRange(Task task, String assignTo, long startDate, long endDate, long today) {
        if (task == null) return false;

        // Exclude tasks whose status/lifecycle is Completed
        if (task.getStatus() != null && LifecycleName.COMPLETED.equalsIgnoreCase(task.getStatus())) {
            return false;
        }
        if (task.getLifecycle() != null &&
                LifecycleName.COMPLETED.equalsIgnoreCase(task.getLifecycle().getLifecycleName())) {
            return false;
        }

        // Check main task
        if (assignTo.equals(task.getAssignTo()) && isTaskInDateRange(task.getDueDate(), startDate, endDate, today)) {
            return true;
        }

        // Check subtasks recursively
        if (task.getSubTasks() != null) {
            for (Task subTask : task.getSubTasks()) {
                if (isTaskAssignedAndInRange(subTask, assignTo, startDate, endDate, today)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Determines if task is in the date range or in the past
     */
    private boolean isTaskInDateRange(Long taskDueDate, long startDate, long endDate, long today) {
        if (taskDueDate == null) return false;

        // Include past tasks
        if (taskDueDate < today) return true;

        // Include tasks from today to endDate
        return taskDueDate >= today && taskDueDate <= endDate;
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

    /**
     * Maps Opportunity entity to DTO
     */
    private OpportunityDTO mapOpportunityToDTO(Opportunity opportunity) {
        OpportunityDTO dto = new OpportunityDTO();
        dto.setOpportunityId(opportunity.getOpportunityId());
        dto.setOpportunityName(opportunity.getOpportunityName());
        dto.setDescription(opportunity.getDescription());
        dto.setRequirement(opportunity.getRequirement());
        dto.setCustomer(opportunity.getCustomer());
        dto.setEstimateRevenue(opportunity.getEstimateRevenue());
        dto.setCurrency(opportunity.getCurrency());
        dto.setProjectTitle(opportunity.getProjectTitle());
        dto.setBusinessUnit(opportunity.getBusinessUnit());
        dto.setCreatedOn(opportunity.getCreatedOn());
        dto.setWorkspaceId(opportunity.getWorkspace().getWorkspaceId());
        dto.setPath(opportunity.getPath());
        return dto;
    }

    /**
     * Returns the start of the day at 1:00 AM UTC for the given user date.
     */
    public long getStartOfDayGMT(int year, int month, int day) {
        LocalDate userDate = LocalDate.of(year, month, day);
        return userDate.atTime(1, 0, 0)
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
    }



}
