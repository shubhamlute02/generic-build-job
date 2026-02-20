package com.accrevent.radius.service;

import com.accrevent.radius.controller.LifecycleController;
import com.accrevent.radius.dto.*;
import com.accrevent.radius.model.*;
import com.accrevent.radius.repository.*;
import com.accrevent.radius.util.LifecyclePromoteConstants;
import com.accrevent.radius.util.RadiusConstants;
import com.accrevent.radius.util.RadiusUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.accrevent.radius.util.CampaignType.DEFAULT;
import static com.accrevent.radius.util.CampaignType.OUTREACH;
import static com.accrevent.radius.util.RadiusUtil.stringtoZonedDateTime;
import org.hibernate.query.criteria.internal.*;

@Slf4j
@Service
public class LifecycleService {
    private final Logger logger = LoggerFactory.getLogger(LifecycleController.class);
    private final LifecycleRepository lifecycleRepository;
    private final CampaignRepository campaignRepository;
    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final CommentsRepository commentsRepository;
    private final VersionRepository versionRepository;
    private final SharePointService sharePointService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private LeadService leadService;
    @Autowired
    private OpportunityService opportunityService;

    @PersistenceContext
    private EntityManager entityManager;

    public LifecycleService(LifecycleRepository lifecycleRepository,
                            CampaignRepository campaignRepository,
                            LeadRepository leadRepository,
                            OpportunityRepository opportunityRepository,
                            CommentsRepository commentsRepository, VersionRepository versionRepository, SharePointService sharePointService)
    {
        this.lifecycleRepository = lifecycleRepository;
        this.campaignRepository = campaignRepository;
        this.leadRepository = leadRepository;
        this.opportunityRepository = opportunityRepository;
        this.commentsRepository = commentsRepository;
        this.versionRepository = versionRepository;
        this.sharePointService = sharePointService;
    }

    public LifecycleDTO createLifecycle(LifecycleDTO lifecycleDTO)
    {
        Lifecycle lifecycle = transformToLifecycle(lifecycleDTO);
        return transformToLifecycleDTO(lifecycleRepository.save(lifecycle));
    }

    public List<Lifecycle> getAllLifecycle()
    {
        return lifecycleRepository.findAll();
    }

    public List<Lifecycle> getLifecyclesByType(String type) {
        return lifecycleRepository.findByTypeIgnoreCase(type);
    }


//    public List<Lifecycle> getLifecycleByCampaignId(Long campaignId)
//    {
//       return lifecycleRepository.findByCampaign_CampaignId(campaignId);
//    }

    public List<Lifecycle> getLifecycleByCampaignId(Long campaignId, String type) {
         if (type == null) {
              return lifecycleRepository.findByCampaign_CampaignId(campaignId);
           }
        return lifecycleRepository.findByCampaign_CampaignIdAndTypeIgnoreCase(campaignId, type);

    }




    public List<Lifecycle> getLifecycleByCampaignIdAndType(Long campaignId, String type) {
        return lifecycleRepository.findByCampaign_CampaignIdAndTypeIgnoreCase(campaignId, type);
    }


    public List<Lifecycle> getLifecycleByOpportunityId(Long opportunityId)
    {
        return lifecycleRepository.findByOpportunity_OpportunityId(opportunityId);
    }

    public List<Lifecycle> getLifecycleByVersionId(Long versionId)
    {
        return lifecycleRepository.findByVersion_versionId(versionId);
    }

    public List<Lifecycle> getLifecycleByLeadId(Long leadId)
    {
        return lifecycleRepository.findByLead_LeadId(leadId);
    }

    public Optional<Lifecycle> getLifecycleById(Long id)
    {
        return lifecycleRepository.findById(id);
    }

    public boolean deleteLifecycle(Long id)
    {
        if(lifecycleRepository.existsById(id))
        {
            lifecycleRepository.deleteById(id);
            return true;
        }
        return false;
    }

    //passes active in api means make passses lifecycle active if it has already inactive status in db
    @Transactional
    public LifecycleStatusUpdateDTO updateLifeCycleStatus(Long lifeCycleId,String status,String previouseLifecycle){
        boolean isSystemComment = false;
        try{

            if(status.equals("active")) {
               Optional<Lifecycle>existingLifecycleOpt = lifecycleRepository.findById(lifeCycleId);
                if(!existingLifecycleOpt.isPresent())
                {
                    throw new IllegalArgumentException("life cycle does not exist");
                }

                Lifecycle existingLifecycle = existingLifecycleOpt.get();

                //added new logic for type validation
                // Validate lifecycle type matches with campaign type
                if (existingLifecycle.getCampaign() != null) {
                    Campaign campaign = existingLifecycle.getCampaign();

                    String campaignType = campaign.getType(); // e.g., "OUTREACH" or "DEFAULT"
                    String lifecycleType = existingLifecycle.getType(); // e.g., "OUTREACH" or "DEFAULT"

                    if ("OUTREACH".equalsIgnoreCase(campaignType) && !"OUTREACH".equalsIgnoreCase(lifecycleType)) {
                        throw new IllegalArgumentException("Outreach campaign must use OUTREACH lifecycle.");
                    }

                    if (!"OUTREACH".equalsIgnoreCase(campaignType) && "OUTREACH".equalsIgnoreCase(lifecycleType)) {
                        throw new IllegalArgumentException("Default campaign cannot use OUTREACH lifecycle.");
                    }
                }

                logger.info("previouseLifecycle{}",previouseLifecycle);
                logger.info("next {} ",existingLifecycle.getLifecycleName());

                if(existingLifecycle.getStatus().equals("inActive"))
               {
                   StringBuilder changes = new StringBuilder();
                   String username = RadiusUtil.getCurrentUsername();
                   String entityType = "";

                   // Build context-aware comment
                   if (existingLifecycle.getCampaign() != null) {
                       entityType = "Campaign";
                   } else if (existingLifecycle.getOpportunity() != null) {
                       entityType = "Opportunity";
                   } else if (existingLifecycle.getLead() != null) {
                       entityType = "Lead";
                   }

                   changes.append(username)
                           .append(" Promoted the ")
                           .append(entityType)
                           .append(" from '")
                           .append(previouseLifecycle) //  This comes from your API param
                           .append("' to '")
                           .append(existingLifecycle.getLifecycleName())
                           .append("'.");

                   Comments comment = new Comments();
                   comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
                   comment.setCommentDescription(changes.toString());
                   comment.setCreatedOn(System.currentTimeMillis());
                   System.out.println("time: " + System.currentTimeMillis());
                   comment.setIsSystemComment(true);

                   if(existingLifecycle.getCampaign() != null)
                   {
                        comment.setCampaign(existingLifecycle.getCampaign());
                   }
                   else if(existingLifecycle.getOpportunity() != null)
                   {
                       comment.setOpportunity(existingLifecycle.getOpportunity());

                   }else if(existingLifecycle.getLead() != null)
                   {
                        comment.setLead(existingLifecycle.getLead());
                   }
                   commentsRepository.save(comment);
                   isSystemComment = true;
               }
            }
            lifecycleRepository.updateLifeCycleStatus(lifeCycleId,status);
            return new LifecycleStatusUpdateDTO("Status field updated Successfully.", isSystemComment);

        }catch (Exception e)
        {
            return new LifecycleStatusUpdateDTO("Error = " + e.getMessage(), false);

        }
    }


@Transactional
public LifecycleStatusUpdateDTO promoteMarketingContentVersion(Long versionId, String newLifecycle) {
    boolean isSystemComment = false;

    try {
        // Fetch all lifecycles for this Version
        List<Lifecycle> lifecycles = lifecycleRepository.findByVersion_versionId(versionId);
        if (lifecycles.isEmpty()) {
            throw new IllegalArgumentException("No lifecycle found for versionId " + versionId);
        }

        // Find the target lifecycle to promote
        Optional<Lifecycle> targetOpt = lifecycles.stream()
                .filter(lc -> lc.getLifecycleName().equalsIgnoreCase(newLifecycle))
                .findFirst();

        if (!targetOpt.isPresent()) {
            throw new IllegalArgumentException("Lifecycle '" + newLifecycle + "' not found for versionId " + versionId);
        }

        Lifecycle targetLifecycle = targetOpt.get();

        // Fetch Version entity once
        Version version = versionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found with ID: " + versionId));

        //  Deactivate currently active lifecycle (if any)
        lifecycles.stream()
                .filter(lc -> LifecyclePromoteConstants.ACTIVE.equalsIgnoreCase(lc.getStatus()))
                .forEach(lc -> lifecycleRepository.updateLifeCycleStatus(lc.getLifecycleId(), "inActive"));

        // Promote target lifecycle if it was inactive
        if (LifecyclePromoteConstants.INACTIVE.equalsIgnoreCase(targetLifecycle.getStatus())) {
            String username = RadiusUtil.getCurrentUsername();
            StringBuilder changes = new StringBuilder();

            changes.append(username)
                    .append(" Promoted the Version")
                    .append("' to '")
                    .append(targetLifecycle.getLifecycleName())
                    .append("'.");

            // Save system comment
            Comments comment = new Comments();
            comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
            comment.setCommentDescription(changes.toString());
            comment.setCreatedOn(System.currentTimeMillis());
            comment.setIsSystemComment(true);
            comment.setVersion(version);
            commentsRepository.save(comment);

            isSystemComment = true;
        }

        //  Activate target lifecycle
        lifecycleRepository.updateLifeCycleStatus(targetLifecycle.getLifecycleId(), LifecyclePromoteConstants.ACTIVE);

        //  Update Version entity fully
        version.setStatus(targetLifecycle.getLifecycleName());
        version.setApprovedAt(System.currentTimeMillis());

        //  Handle SharePoint folder renaming
        String oldUrl = version.getSharepointUrl();
        log.info("========================================oldUrl: {}", oldUrl);


        String newFolderName = version.getVersion() + " " + targetLifecycle.getLifecycleName();
        log.info("========================================newFolderName: {}", newFolderName);


        if (oldUrl != null && !oldUrl.isBlank()) {
            try {
                SharePointURLDTO folder = sharePointService.resolveFolderFromUrl(oldUrl);

                log.info("PROMOTE :: Resolved folder name = {}", folder.getName());

                log.info("PROMOTE :: Renaming folder [{}] to [{}]",
                        folder.getName(),
                        newFolderName);

                sharePointService.renameFolderById(folder.getParentDriveId(), folder.getId(), newFolderName);

                log.info("========================================old Path: {}", version.getSharepointPath());
                System.out.println(version.getVersion()+"version.getVersion()");

                String encodedName = URLEncoder.encode(newFolderName, StandardCharsets.UTF_8)
                        .replace("+", "%20");

                String baseUrl = oldUrl.substring(0, oldUrl.lastIndexOf("/") + 1);
                String newUrl = baseUrl + encodedName;

                version.setSharepointUrl(newUrl);

                String oldPath = version.getSharepointPath();

                String newPath;
                if (oldPath != null && oldPath.contains("/")) {
                    newPath = oldPath.substring(0, oldPath.lastIndexOf("/") + 1) + newFolderName;
                } else {
                    newPath = newFolderName;
                }

                version.setSharepointPath(newPath);
                log.info("Updated SharePoint path: {}", newPath);

                log.info("========================================newUrl: {}", newUrl);

               logger.info(" Version folder renamed in SharePoint: {}", newUrl);
            } catch (Exception ex) {
                System.err.println("Failed to rename SharePoint folder for version: " + ex.getMessage());
            }
        }

        //  Save version entity (status + approvedAt + path + url) in one go
        versionRepository.save(version);

        //  Return success
        return new LifecycleStatusUpdateDTO(
                "Version lifecycle promoted successfully to " + newLifecycle, isSystemComment);

    } catch (Exception e) {
        return new LifecycleStatusUpdateDTO("Error = " + e.getMessage(), false);
    }
}



    private Lifecycle transformToLifecycle(LifecycleDTO lifecycleDTO)
    {
        Lifecycle lifecycle = new Lifecycle();
        lifecycle.setLifecycleName(lifecycleDTO.getLifecycleName());

        Optional<Campaign>campaign = campaignRepository.findById(lifecycleDTO.getCampaignId());
        Optional<Lead> lead = leadRepository.findById(lifecycleDTO.getLeadId());
        Optional<Opportunity> opportunity = opportunityRepository.findById(lifecycleDTO.getOpportunityId());
        Optional<Version> version = versionRepository.findById(lifecycleDTO.getVersionId());
        if(lifecycleDTO.getLifecycleId()!= null)
        {
            lifecycle.setLifecycleId(lifecycle.getLifecycleId());
        }

        if (campaign.isPresent()) {
            Campaign c = campaign.get();
            if (c.getType().equals(OUTREACH) && !OUTREACH.equalsIgnoreCase(lifecycleDTO.getType())) {
                throw new IllegalArgumentException("OUTREACH Campaign must use OUTREACH lifecycle.");
            }
            if (!c.getType().equals(OUTREACH) && !DEFAULT.equalsIgnoreCase(lifecycleDTO.getType())) {
                throw new IllegalArgumentException("DEFAULT Campaign must use DEFAULT lifecycle.");
            }
            lifecycle.setCampaign(c);
            lifecycle.setType(lifecycleDTO.getType());

        }else if(lead.isPresent())
        {
            lifecycle.setLead(lead.get());
            lifecycle.setType(DEFAULT);
        }else if(opportunity.isPresent())
        {
            lifecycle.setOpportunity(opportunity.get());
            lifecycle.setType(DEFAULT);
        }else if (version.isPresent()) {
            lifecycle.setVersion(version.get());
            lifecycle.setType(DEFAULT); // or decide type rules for Version
        }
        else {
            throw new IllegalArgumentException("could not find any id in between campaign, lead,Opportunity");
        }

        return lifecycle;

    }




    private LifecycleDTO transformToLifecycleDTO(Lifecycle lifecycle)
    {
        LifecycleDTO lifecycleDTO = new LifecycleDTO();
        lifecycleDTO.setLifecycleId(lifecycle.getLifecycleId());
        lifecycleDTO.setLifecycleName(lifecycle.getLifecycleName());
        // Safe null checks for all relationships
        if (lifecycle.getCampaign() != null) {
            lifecycleDTO.setCampaignId(lifecycle.getCampaign().getCampaignId());
        }
        if (lifecycle.getLead() != null) {
            lifecycleDTO.setLeadId(lifecycle.getLead().getLeadId());
        }
        if (lifecycle.getOpportunity() != null) {
            lifecycleDTO.setOpportunityId(lifecycle.getOpportunity().getOpportunityId());
        }
        if (lifecycle.getVersion() != null) {
        lifecycleDTO.setVersionId(lifecycle.getVersion().getVersionId());
    }

        lifecycleDTO.setType(lifecycle.getType());

        return lifecycleDTO;
    }




    public LeadLifecycleCountDTO getLeadPipelineReport(Long workspaceId, Long campaignId, String startTime, String endTime) {
        ZonedDateTime startDate = null;
        ZonedDateTime endDate = null;

        if (startTime != null && endTime != null) {
            startDate = stringtoZonedDateTime(startTime);
            endDate = stringtoZonedDateTime(endTime);
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<LeadLifecycleCountDTO> query = cb.createQuery(LeadLifecycleCountDTO.class);
        Root<Lifecycle> lifecycle = query.from(Lifecycle.class);

        Join<Lifecycle, Lead> lead = lifecycle.join("lead", JoinType.LEFT);
        Join<Lead, Campaign> campaign = lead.join("campaign", JoinType.LEFT);
        Join<Campaign, Workspace> workspace = campaign.join("workspace", JoinType.LEFT);



        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(lifecycle.get("status"), "active"));

        // Add condition to ensure lead is not null if needed
        predicates.add(cb.isNotNull(lifecycle.get("lead")));

        if (workspaceId != null) {
            predicates.add(cb.equal(workspace.get("workspaceId"), workspaceId));
        }

        if (campaignId != null) {
            predicates.add(cb.equal(campaign.get("campaignId"), campaignId));
        }

        if (startDate != null && endDate != null) {
            Predicate campaignDateRange = cb.between(campaign.get("createdOn"), startDate, endDate);
            Predicate leadDateRange = cb.between(lead.get("createdOn"), startDate, endDate);
            predicates.add(cb.or(campaignDateRange, leadDateRange));
        }

        Expression<Long> identifiedCount = cb.coalesce(
                cb.sum(cb.<Long>selectCase()
                        .when(cb.equal(lifecycle.get("lifecycleName"), "identified"), cb.literal(1L))
                        .otherwise(cb.literal(0L))
                ), 0L);

        Expression<Long> researchCount = cb.coalesce(
                cb.sum(cb.<Long>selectCase()
                        .when(cb.equal(lifecycle.get("lifecycleName"), "research"), cb.literal(1L))
                        .otherwise(cb.literal(0L))
                ), 0L);

        Expression<Long> prospectingCount = cb.coalesce(
                cb.sum(cb.<Long>selectCase()
                        .when(cb.equal(lifecycle.get("lifecycleName"), "prospecting"), cb.literal(1L))
                        .otherwise(cb.literal(0L))
                ), 0L);

        query.select(cb.construct(LeadLifecycleCountDTO.class, identifiedCount, researchCount, prospectingCount));

        query.where(cb.and(predicates.toArray(new Predicate[0])));

        TypedQuery<LeadLifecycleCountDTO> typedQuery = entityManager.createQuery(query);
        return typedQuery.getSingleResult();
    }

    public LeadLifecycleCountDTO getLeadPipelineReport(Long campaignId) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<LeadLifecycleCountDTO> query = cb.createQuery(LeadLifecycleCountDTO.class);
        Root<Lifecycle> lifecycle = query.from(Lifecycle.class);

        Join<Lifecycle, Lead> lead = lifecycle.join("lead", JoinType.LEFT);
        Join<Lead, Campaign> campaign = lead.join("campaign", JoinType.LEFT);
        Join<Campaign, Workspace> workspace = campaign.join("workspace", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(lifecycle.get("status"), "active"));

        if (campaignId != null) {
            predicates.add(cb.equal(campaign.get("campaignId"), campaignId));
        }

        Expression<Long> identifiedCount = cb.coalesce(
                cb.sum(cb.<Long>selectCase()
                        .when(cb.equal(lifecycle.get("lifecycleName"), "identified"), cb.literal(1L))
                        .otherwise(cb.literal(0L))
                ), 0L);

        Expression<Long> researchCount = cb.coalesce(
                cb.sum(cb.<Long>selectCase()
                        .when(cb.equal(lifecycle.get("lifecycleName"), "research"), cb.literal(1L))
                        .otherwise(cb.literal(0L))
                ), 0L);

        Expression<Long> prospectingCount = cb.coalesce(
                cb.sum(cb.<Long>selectCase()
                        .when(cb.equal(lifecycle.get("lifecycleName"), "prospecting"), cb.literal(1L))
                        .otherwise(cb.literal(0L))
                ), 0L);

        query.select(cb.construct(LeadLifecycleCountDTO.class, identifiedCount, researchCount, prospectingCount));

        query.where(cb.and(predicates.toArray(new Predicate[0])));

        TypedQuery<LeadLifecycleCountDTO> typedQuery = entityManager.createQuery(query);
        return typedQuery.getSingleResult();
    }

    public OpportunityLifecycleCountDTO getOpportunityPipelineReport(
            Long workspaceId, Long opportunityId, String  startTime, String endTime) {

        ZonedDateTime startDate = null;
        ZonedDateTime endDate = null;
        if(startTime!= null && endTime != null)
        {
            startDate = stringtoZonedDateTime(startTime);
            endDate = stringtoZonedDateTime(endTime);
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<OpportunityLifecycleCountDTO> query = cb.createQuery(OpportunityLifecycleCountDTO.class);
        Root<Lifecycle> lifecycle = query.from(Lifecycle.class);

        Join<Lifecycle, Opportunity> opportunity = lifecycle.join("opportunity", JoinType.LEFT);
        Join<Opportunity, Workspace> workspace = opportunity.join("workspace", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(lifecycle.get("status"), "active"));

        if (workspaceId != null) {
            predicates.add(cb.equal(workspace.get("workspaceId"), workspaceId));
        }

        if (opportunityId != null) {
            predicates.add(cb.equal(opportunity.get("opportunityId"), opportunityId));
        }

        if (startDate != null && endDate != null) {
            predicates.add(cb.between(opportunity.get("createdOn"), startDate, endDate));
        }

        Expression<Long> discoveryCount = cb.coalesce(
                cb.sum(cb.<Long>selectCase()
                        .when(cb.equal(lifecycle.get("lifecycleName"), "discovery"), cb.literal(1L))
                        .otherwise(cb.literal(0L))
                ), 0L);

        Expression<Long> proposalCount = cb.coalesce(
                cb.sum(cb.<Long>selectCase()
                        .when(cb.equal(lifecycle.get("lifecycleName"), "proposal"), cb.literal(1L))
                        .otherwise(cb.literal(0L))
                ), 0L);

        Expression<Long> customerEvaluatingCount = cb.coalesce(
                cb.sum(cb.<Long>selectCase()
                        .when(cb.equal(lifecycle.get("lifecycleName"), "customer evaluating"), cb.literal(1L))
                        .otherwise(cb.literal(0L))
                ), 0L);

        Expression<Long> closedWonCount = cb.coalesce(
                cb.sum(cb.<Long>selectCase()
                        .when(cb.equal(lifecycle.get("lifecycleName"), "closed won"), cb.literal(1L))
                        .otherwise(cb.literal(0L))
                ), 0L);


        query.select(cb.construct(OpportunityLifecycleCountDTO.class,
                discoveryCount, proposalCount, customerEvaluatingCount, closedWonCount));

        query.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<OpportunityLifecycleCountDTO> typedQuery = entityManager.createQuery(query);
        return typedQuery.getSingleResult();
    }

//    public List<Lead> getLeadsForPipelineByBUandLCStatus(String businessUnitName, String lifeCycleName) throws Exception {
//        StringBuilder querySB = new StringBuilder();
//        Long leadId;
//        List<Lead> leadList = new ArrayList<Lead>();
//
//        querySB.append("SELECT lead_id FROM lead ");
//        querySB.append("where business_unit='");
//        querySB.append(businessUnitName);
//        querySB.append("' ");
//        querySB.append("intersect ");
//        querySB.append("SELECT lead_id FROM lifecycle ");
//        querySB.append("where status='active' and lifecycle_name='");
//        querySB.append(lifeCycleName);
//        querySB.append("'");
//
//        logger.debug("querySB.toString() = " + querySB.toString());
//        System.out.println("HHHH HHHH HHHH HHHH querySB.toString() = " + querySB.toString());
//
//        Connection con = jdbcTemplate.getDataSource().getConnection();
//        Statement stmt = con.createStatement();
//        ResultSet rs = stmt.executeQuery(querySB.toString());
//
//        for(int i = 1; rs.next(); i++)
//        {
//            leadId = rs.getLong("lead_id");
//            System.out.println(i +  " > " + leadId);
//
//            leadList.add(leadService.getLeadById(leadId).get());
//        }
//
//        con.close();
//
//        return leadList;
//    }


    public List<LeadDTO> getLeadsForPipelineByBUandLCStatus(String businessUnitName, String lifeCycleName) throws Exception {
        StringBuilder querySB = new StringBuilder();
        List<LeadDTO> leadDTOList = new ArrayList<>();

        querySB.append("SELECT lead_id FROM lead ");
        querySB.append("WHERE business_unit='").append(businessUnitName).append("' ");
        querySB.append("INTERSECT ");
        querySB.append("SELECT lead_id FROM lifecycle ");
        querySB.append("WHERE status='active' AND lifecycle_name='").append(lifeCycleName).append("'");

        logger.debug("querySB.toString() = " + querySB.toString());
        System.out.println("HHHH HHHH HHHH HHHH querySB.toString() = " + querySB);

        try (Connection con = jdbcTemplate.getDataSource().getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(querySB.toString())) {

            int i = 1;
            while (rs.next()) {
                Long leadId = rs.getLong("lead_id");
                System.out.println(i++ + " > " + leadId);

                Lead lead = leadService.getLeadById(leadId).orElse(null);
                if (lead != null) {
                    LeadDTO dto = leadService.transformToLeadDTO(lead);
                    dto.setPath(leadService.buildFullLeadPath(lead)); //lead path gets Added.
                    leadDTOList.add(dto);
                }
            }
        }

        return leadDTOList;
    }


//    public List<Opportunity> getOpportunitiesForPipelineByBUandLCStatus(String businessUnitName, String lifeCycleName) throws Exception {
//        StringBuilder querySB = new StringBuilder();
//        Long opportunityId;
//        List<Opportunity> opportunityList = new ArrayList<>();
//
//        querySB.append("SELECT opportunity_id FROM opportunity ");
//        querySB.append("WHERE business_unit='").append(businessUnitName).append("' ");
//        querySB.append("INTERSECT ");
//        querySB.append("SELECT opportunity_id FROM lifecycle ");
//        querySB.append("WHERE status='active' AND lifecycle_name='").append(lifeCycleName).append("'");
//
//        logger.debug("Query: " + querySB.toString());
//
//        try (Connection con = jdbcTemplate.getDataSource().getConnection();
//             Statement stmt = con.createStatement();
//             ResultSet rs = stmt.executeQuery(querySB.toString())) {
//
//            int i = 1;
//            while (rs.next()) {
//                opportunityId = rs.getLong("opportunity_id");
//                System.out.println(i++ + " > " + opportunityId);
//                opportunityList.add(opportunityService.getOpportunityById(opportunityId).get());
//            }
//        }
//
//        return opportunityList;
//    }


    public List<Opportunity> getOpportunitiesForPipelineByBUandLCStatus(String businessUnitName, String lifeCycleName) throws Exception {
        StringBuilder querySB = new StringBuilder();
        Long opportunityId;
        List<Opportunity> opportunityList = new ArrayList<>();

        querySB.append("SELECT opportunity_id FROM opportunity ");
        querySB.append("WHERE business_unit='").append(businessUnitName).append("' ");
        querySB.append("INTERSECT ");
        querySB.append("SELECT opportunity_id FROM lifecycle ");
        querySB.append("WHERE status='active' AND lifecycle_name='").append(lifeCycleName).append("'");

        logger.debug("Query: " + querySB.toString());

        try (Connection con = jdbcTemplate.getDataSource().getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(querySB.toString())) {

            int i = 1;
            while (rs.next()) {
                opportunityId = rs.getLong("opportunity_id");
                System.out.println(i++ + " > " + opportunityId);

                Opportunity opportunity = opportunityService.getOpportunityById(opportunityId).orElse(null);
                if (opportunity != null) {
                    // set path without changing JSON structure
                    opportunity.setPath(opportunityService.buildPathForOpportunity(opportunityId));
                    opportunityList.add(opportunity);
                }
            }
        }

        return opportunityList;
    }


    public int getLeadCount(String businessUnitName, String lifeCycleName) throws Exception {
        StringBuilder querySB = new StringBuilder();
        int count;

        querySB.append("SELECT lead_id FROM lead ");
        querySB.append("where business_unit='");
        querySB.append(businessUnitName);
        querySB.append("' ");
        querySB.append("intersect ");
        querySB.append("SELECT lead_id FROM lifecycle ");
        querySB.append("where status='active' and lifecycle_name='");
        querySB.append(lifeCycleName);
        querySB.append("'");

        logger.debug("querySB.toString() = " + querySB.toString());

        Connection con = jdbcTemplate.getDataSource().getConnection();
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(querySB.toString());

        for(count = 0; rs.next(); count++);

        con.close();

        return count;
    }

    public int getOpportunityCount(String businessUnitName, String lifeCycleName) throws Exception {
        StringBuilder querySB = new StringBuilder();
        int count;

        querySB.append("SELECT opportunity_id FROM opportunity ");
        querySB.append("WHERE business_unit='").append(businessUnitName).append("' ");
        querySB.append("INTERSECT ");
        querySB.append("SELECT opportunity_id FROM lifecycle ");
        querySB.append("WHERE status='active' AND lifecycle_name='").append(lifeCycleName).append("'");

        logger.debug("querySB.toString() = " + querySB.toString());

        try (Connection con = jdbcTemplate.getDataSource().getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(querySB.toString())) {

            for (count = 0; rs.next(); count++);
        }

        return count;
    }



}
