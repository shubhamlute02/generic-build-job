package com.accrevent.radius.service;

import com.accrevent.radius.dto.ProgressReportForBussinessUnitDTO;
import com.accrevent.radius.model.*;
import com.accrevent.radius.repository.*;
import com.accrevent.radius.util.LifecycleType;
import com.accrevent.radius.util.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProgressReportForBussinessUnitService {
    private static final Logger logger = LoggerFactory.getLogger(ProgressReportForBussinessUnitService.class);

    private final CampaignRepository campaignRepository;
    private final LeadRepository leadRepository;
    private final TaskRepository taskRepository;
    private final LifecycleRepository lifecycleRepository;
    private final ConstantLifecycleRepository constantLifecycleRepository;

    public ProgressReportForBussinessUnitService(CampaignRepository campaignRepository, LeadRepository leadRepository, TaskRepository taskRepository, LifecycleRepository lifecycleRepository, ConstantLifecycleRepository constantLifecycleRepository) {
        this.campaignRepository = campaignRepository;
        this.leadRepository = leadRepository;
        this.taskRepository = taskRepository;
        this.lifecycleRepository = lifecycleRepository;
        this.constantLifecycleRepository = constantLifecycleRepository;
    }


        public ProgressReportForBussinessUnitDTO getProgressReportForBU(String campaignId) {
        logger.info("Progress Report for Campaign ID:{}", campaignId);

        Long campaignIdLong = Long.parseLong(campaignId);

        ProgressReportForBussinessUnitDTO progressReport = new ProgressReportForBussinessUnitDTO();

        //get campaign Details.
        progressReport.setCampaignId(campaignId);

        Campaign campaign = campaignRepository.findById(campaignIdLong)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found with given ID" + campaignId));

        progressReport.setActualStartDate(campaign.getActualStartDate());
        progressReport.setActualEndDate(campaign.getActualEndDate());
        progressReport.setPlannedStartDate(campaign.getPlannedStartDate());
        progressReport.setPlannedEndDate(campaign.getPlannedEndDate());

        //No of targeted Companies
        List<Lead> leads = leadRepository.findByCampaign_CampaignId(campaignIdLong);
        progressReport.setNoOfTargetCompanies(leads.size());

            // --- Contact Counts by Outreach Type ---
            Map<String, Integer> contactCounts = new HashMap<>();
            contactCounts.put("email", taskRepository.countByLifecycle_Lead_Campaign_CampaignIdAndType(campaignIdLong,TaskType.EMAIL_OUTREACH_TASK));
            contactCounts.put("linkedIn", taskRepository.countByLifecycle_Lead_Campaign_CampaignIdAndType(campaignIdLong,TaskType.LINKEDIN_OUTREACH_TASK));
            contactCounts.put("phone", taskRepository.countByLifecycle_Lead_Campaign_CampaignIdAndType(campaignIdLong,TaskType.PHONE_OUTREACH_TASK));
            progressReport.setNoOfContacts(contactCounts);


            // --- Lifecycle Counts for Each Task Type ---
            Map<String, Map<String, Integer>> lifecycleCountsByType = new LinkedHashMap<>();

            lifecycleCountsByType.put("EMAIL_OUTREACH", getTaskLifecycleCounts(campaignIdLong, 5L,TaskType.EMAIL_OUTREACH_TASK));
            lifecycleCountsByType.put("LINKEDIN_OUTREACH", getTaskLifecycleCounts(campaignIdLong, 10L,TaskType.LINKEDIN_OUTREACH_TASK));
            lifecycleCountsByType.put("PHONE_OUTREACH", getTaskLifecycleCounts(campaignIdLong, 11L,TaskType.PHONE_OUTREACH_TASK));

            progressReport.setLifecycleCounts(lifecycleCountsByType);

            return progressReport;
    }


        private Map<String, Integer> getTaskLifecycleCounts(Long campaignId,Long cycleId, String taskType) {
        // Get valid outreach states from DB (like: "not started", "intro", etc.)
        List<String> validOutreachStates = constantLifecycleRepository.findByCycleId(cycleId)
                .stream()
                .map(ConstantLifecycle::getCycleName)
                .collect(Collectors.toList());


        // Initialize the counts for each valid state
        Map<String, Integer> lifecycleCounts = new LinkedHashMap<>();
        validOutreachStates.forEach(state -> lifecycleCounts.put(state, 0));

        // Fetch all tasks with their lifecycle info
        List<Task> tasks = taskRepository.findByCampaignIdAndTypeWithLifecycle(campaignId,taskType);

        for (Task task : tasks) {
            Lifecycle lifecycle = task.getLifecycle();

            if (lifecycle == null) {
                continue;
            }

            // Check if lifecycle type is "outreach"
            if (!LifecycleType.OUTREACH.equalsIgnoreCase(lifecycle.getType())) {
                continue;
            }

            // Get task status (e.g., "Not Started", "Intro", etc.)
            String taskStatus = task.getStatus();
            if (taskStatus != null) {
                // Find matching valid state (case-insensitive)
                String matchedState = validOutreachStates.stream()
                        .filter(state -> state.equalsIgnoreCase(taskStatus))
                        .findFirst()
                        .orElse(null);

                if (matchedState != null) {
                    lifecycleCounts.put(matchedState, lifecycleCounts.get(matchedState) + 1);
                }
            }
        }
        return lifecycleCounts;
    }


//if needed for normal task as well.
//    public ProgressReportForBussinessUnitDTO getProgressReportForBU(String campaignId) {
//        logger.info("Progress Report for Campaign ID:{}", campaignId);
//
//        Long campaignIdLong = Long.parseLong(campaignId);
//
//        ProgressReportForBussinessUnitDTO progressReport = new ProgressReportForBussinessUnitDTO();
//
//        // Get campaign Details
//        progressReport.setCampaignId(campaignId);
//
//        Campaign campaign = campaignRepository.findById(campaignIdLong)
//                .orElseThrow(() -> new IllegalArgumentException("Campaign not found with given ID" + campaignId));
//
//        progressReport.setActualStartDate(campaign.getActualStartDate());
//        progressReport.setActualEndDate(campaign.getActualEndDate());
//        progressReport.setPlannedStartDate(campaign.getPlannedStartDate());
//        progressReport.setPlannedEndDate(campaign.getPlannedEndDate());
//
//        // No of targeted Companies
//        List<Lead> leads = leadRepository.findByCampaign_CampaignId(campaignIdLong);
//        progressReport.setNoOfTargetCompanies(leads.size());
//
//        // No of contacts
//        int totalContact = taskRepository.countByCampaignId(campaignIdLong);
//        progressReport.setNoOfContacts(totalContact);
//
//        // Determine which task type (outreach or regular) this campaign uses
//        Long cycleId = determineTaskTypeForCampaign(campaignIdLong);
//
//        Map<String, Integer> taskLifecycleCounts = getTaskLifecycleCounts(campaignIdLong, cycleId);
//        progressReport.setLifecycleCounts(taskLifecycleCounts);
//
//        return progressReport;
//    }
//
//    private Long determineTaskTypeForCampaign(Long campaignId) {
//        // Fetch all tasks for this campaign
//        List<Task> tasks = taskRepository.findByCampaignIdWithLifecycle(campaignId);
//
//        // Check if any task has outreach status (cycleId 5L)
//        boolean hasOutreachTasks = tasks.stream()
//                .anyMatch(task -> {
//                    if (task.getLifecycle() == null) return false;
//                    String status = task.getStatus();
//                    return status != null && isStatusInCycle(status, 5L);
//                });
//
//        logger.info("hasOutreachTasks{}",hasOutreachTasks);
//
//
//        if (hasOutreachTasks) {
//            return 5L; // Use outreach task statuses
//        }
//
//        // Default to regular task statuses (cycleId 6L)
//        return 6L;
//    }
//
//    private boolean isStatusInCycle(String status, Long cycleId) {
//        // Get all statuses for the given cycleId
//        List<String> validStatuses = constantLifecycleRepository.findByCycleId(cycleId)
//                .stream()
//                .map(ConstantLifecycle::getCycleName)
//                .collect(Collectors.toList());
//
//        // Check if the status exists in this cycle (case-insensitive)
//        return validStatuses.stream()
//                .anyMatch(s -> s.equalsIgnoreCase(status));
//    }
//
//    private Map<String, Integer> getTaskLifecycleCounts(Long campaignId, Long cycleId) {
//        // Get valid states for the specified cycleId from DB
//        List<String> validStates = constantLifecycleRepository.findByCycleId(cycleId)
//                .stream()
//                .map(ConstantLifecycle::getCycleName)
//                .collect(Collectors.toList());
//
//        logger.info("Valid States from DB for cycle {}: {}", cycleId, validStates);
//
//        // Initialize the counts for each valid state
//        Map<String, Integer> lifecycleCounts = new LinkedHashMap<>();
//        validStates.forEach(state -> lifecycleCounts.put(state, 0));
//
//        // Fetch all tasks with their lifecycle info
//        List<Task> tasks = taskRepository.findByCampaignIdWithLifecycle(campaignId);
//        logger.info("Tasks Found for Campaign ID {}: {}", campaignId, tasks.size());
//
//        for (Task task : tasks) {
//            Lifecycle lifecycle = task.getLifecycle();
//
//            if (lifecycle == null) {
//                logger.warn("Task ID {} has null lifecycle.", task.getTaskId());
//                continue;
//            }
//
//            // Get task status
//            String taskStatus = task.getStatus();
//            if (taskStatus != null) {
//                // Find matching valid state (case-insensitive)
//                String matchedState = validStates.stream()
//                        .filter(state -> state.equalsIgnoreCase(taskStatus))
//                        .findFirst()
//                        .orElse(null);
//
//                if (matchedState != null) {
//                    lifecycleCounts.put(matchedState, lifecycleCounts.get(matchedState) + 1);
//                }
//            }
//        }
//        logger.info("Final Counts for cycle {}: {}", cycleId, lifecycleCounts);
//        return lifecycleCounts;
//    }
}



