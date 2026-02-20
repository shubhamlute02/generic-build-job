package com.accrevent.radius.service;

import com.accrevent.radius.model.*;
import com.accrevent.radius.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.accrevent.radius.util.TaskType.EMAIL_OUTREACH_TASK;

@Service
public class DataCleansingService {
    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final TaskRepository taskRepository;
    private final CampaignRepository campaignRepository;
    private final LifecycleRepository lifecycleRepository;
    private final EmailOutreachCampaignMaturityRecordRegisterRepository emailOutreachCampaignMaturityRecordRegisterRepository;
    private final PhoneOutreachCampaignMaturityRecordRegisterRepository phoneOutreachCampaignMaturityRecordRegisterRepository;

    public DataCleansingService(LeadRepository leadRepository, OpportunityRepository opportunityRepository, TaskRepository taskRepository, CampaignRepository campaignRepository, LifecycleRepository lifecycleRepository, EmailOutreachCampaignMaturityRecordRegisterRepository emailOutreachCampaignMaturityRecordRegisterRepository, PhoneOutreachCampaignMaturityRecordRegisterService phoneOutreachCampaignMaturityRecordRegisterService, PhoneOutreachCampaignMaturityRecordRegisterRepository phoneOutreachCampaignMaturityRecordRegisterRepository) {
        this.leadRepository = leadRepository;
        this.opportunityRepository = opportunityRepository;
        this.taskRepository = taskRepository;
        this.campaignRepository = campaignRepository;
        this.lifecycleRepository = lifecycleRepository;
        this.emailOutreachCampaignMaturityRecordRegisterRepository = emailOutreachCampaignMaturityRecordRegisterRepository;
        this.phoneOutreachCampaignMaturityRecordRegisterRepository = phoneOutreachCampaignMaturityRecordRegisterRepository;
    }

    @Transactional
    public String updateBusinessUnitIOT() {

        int updatedLeads = leadRepository.updateLeadBusinessUnit();
        int updatedOpportunities = opportunityRepository.updateOpportunityBusinessUnit();

        return String.format("Updated %d Leads and %d Opportunities", updatedLeads, updatedOpportunities);
    }

    @Transactional
    public String updateOutreachTaskType() {
        String constantValue = EMAIL_OUTREACH_TASK; // your constant
        int updatedTasks = taskRepository.updateOutreachTaskType(constantValue);
        return String.format("Updated %d Tasks to %s", updatedTasks, constantValue);
    }

    @Transactional
    public int updatelifecyclestatusConvention() {
        // Old → New status mapping
        String[][] mappings = {
                {"not started", "Not Started"},
                {"Not started", "Not Started"},
                {"Not Started", "Not Started"},
                {"intro", "Intro"},
                {"Intro", "Intro"},
                {"follow up1", "Follow Up 1"},
                {"Follow Up 1", "Follow Up 1"},
                {"follow up2", "Follow Up 2"},
                {"Follow Up 2", "Follow Up 2"},
                {"follow up3", "Follow Up 3"},
                {"Follow Up 3", "Follow Up 3"},
                {"closure", "Closure"},
                {"Closure", "Closure"},
                {"meeting", "Meeting"},
                {"Meeting", "Meeting"},
                {"Complete", "Completed"},
                {"Completed", "Completed"},
                {"in work", "In Work"},
                {"In work", "In Work"},
                {"In Work", "In Work"}
        };

        int totalUpdated = 0;

        for (String[] pair : mappings) {
            String oldStatus = pair[0];
            String newStatus = pair[1];

            // Update task table
            List<Task> tasks = taskRepository.findByStatus(oldStatus);
            if (!tasks.isEmpty()) {
                tasks.forEach(t -> t.setStatus(newStatus));
                taskRepository.saveAll(tasks);
                totalUpdated += tasks.size();
                System.out.println("Updated " + tasks.size() +
                        " tasks from '" + oldStatus + "' to '" + newStatus + "'");
            }


        // Update OutreachCampaignMaturityRecord table
        List<EmailOutreachCampaignMaturityRecordRegister> records = emailOutreachCampaignMaturityRecordRegisterRepository.findByStatus(oldStatus);
        if (!records.isEmpty()) {
            records.forEach(r -> r.setStatus(newStatus));
            emailOutreachCampaignMaturityRecordRegisterRepository.saveAll(records);
            totalUpdated += records.size();
            System.out.println("Updated " + records.size() +
                    " maturity records from '" + oldStatus + "' to '" + newStatus + "'");
        }
    }

        return totalUpdated;
    }

    @Transactional
    public int updateCampaignStatusConvention() {
        // Old → New mapping
        String[][] mappings = {
                {"backlog", "Backlog"},
                {"preparation", "Preparation"},
                {"active", "Active"},
                {"closing", "Closing"},
                {"onHold", "OnHold"},
                {"cancelled", "Cancelled"},
                {"complete", "Completed"},

                {"not started", "Not Started"},
                {"in progress", "In Progress"},
        };

        int totalUpdated = 0;

        for (String[] pair : mappings) {
            String oldStatus = pair[0];
            String newStatus = pair[1];

            List<Lifecycle> lifecycles =
                    lifecycleRepository.findByLifecycleNameAndCampaignIsNotNull(oldStatus);

            if (!lifecycles.isEmpty()) {
                lifecycles.forEach(lc -> lc.setLifecycleName(newStatus));
                lifecycleRepository.saveAll(lifecycles);
                totalUpdated += lifecycles.size();

                System.out.println("Updated " + lifecycles.size() +
                        " campaign lifecycles from '" + oldStatus + "' to '" + newStatus + "'");
            }
        }

        return totalUpdated;
    }


    @Transactional
    public int updateLeadStatusConvention() {
        // Old → New mapping
        String[][] mappings = {
                {"identified", "Identified"},
                {"research", "Research"},
                {"prospecting", "Prospecting"},
                {"disqualified", "Disqualified"},
        };

        int totalUpdated = 0;

        for (String[] pair : mappings) {
            String oldStatus = pair[0];
            String newStatus = pair[1];

            List<Lifecycle> lifecycles =
                    lifecycleRepository.findByLifecycleNameAndLeadIsNotNull(oldStatus);

            if (!lifecycles.isEmpty()) {
                lifecycles.forEach(lc -> lc.setLifecycleName(newStatus));
                lifecycleRepository.saveAll(lifecycles);
                totalUpdated += lifecycles.size();

                System.out.println("Updated " + lifecycles.size() +
                        " lead lifecycles from '" + oldStatus + "' to '" + newStatus + "'");
            }
        }

        return totalUpdated;
    }

    @Transactional
    public int updateOpportunityStatusConvention() {
        // Old → New mapping
        String[][] mappings = {
                {"discovery", "Discovery"},
                {"proposal", "Proposal"},
                {"customer evaluating", "Customer Evaluating"},
                {"closing", "Closing"},
                {"closed won", "Closed Won"},
                {"closed lost", "Closed Lost"},
        };

        int totalUpdated = 0;

        for (String[] pair : mappings) {
            String oldStatus = pair[0];
            String newStatus = pair[1];

            List<Lifecycle> lifecycles =
                    lifecycleRepository.findByLifecycleNameAndOpportunityIsNotNull(oldStatus);

            if (!lifecycles.isEmpty()) {
                lifecycles.forEach(lc -> lc.setLifecycleName(newStatus));
                lifecycleRepository.saveAll(lifecycles);
                totalUpdated += lifecycles.size();

                System.out.println("Updated " + lifecycles.size() +
                        " opportunity lifecycles from '" + oldStatus + "' to '" + newStatus + "'");
            }
        }

        return totalUpdated;
    }

    public void changeCallingToCalled() {

        List<PhoneOutreachCampaignMaturityRecordRegister> records =
                phoneOutreachCampaignMaturityRecordRegisterRepository.findAll();

        for (PhoneOutreachCampaignMaturityRecordRegister record : records) {

            if ("Calling".equals(record.getStatus())) {
                record.setStatus("Called");
            }
        }

        phoneOutreachCampaignMaturityRecordRegisterRepository.saveAll(records);
    }

    @Transactional
    public void closingToNegotiating() {

        // Fetch all opportunities
        List<Opportunity> opportunities = opportunityRepository.findAll();

        for (Opportunity opportunity : opportunities) {

            List<Lifecycle> lifecycles = opportunity.getOpportunitylifecycleList();

            for (Lifecycle lifecycle : lifecycles) {

                // Check lifecycle name and type (case-insensitive)
                if ("Closing".equalsIgnoreCase(lifecycle.getLifecycleName())
                        && "default".equalsIgnoreCase(lifecycle.getType())) {

                    // Replace lifecycle name
                    lifecycle.setLifecycleName("Negotiating");
                }
            }
        }

        // Save all opportunities (cascade updates lifecycles)
        opportunityRepository.saveAll(opportunities);
    }




}
