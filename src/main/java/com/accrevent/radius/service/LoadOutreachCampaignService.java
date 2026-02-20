package com.accrevent.radius.service;

import com.accrevent.radius.dto.LeadDTO;
import com.accrevent.radius.dto.LoadOutreachCampaignResponseDTO;
import com.accrevent.radius.model.*;
import com.accrevent.radius.repository.*;
import com.accrevent.radius.util.RadiusConstants;
import com.accrevent.radius.util.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class LoadOutreachCampaignService {

    private static final Logger logger = LoggerFactory.getLogger(LoadOutreachCampaignService.class);

    private final ContactRepository contactRepository;
    private final LeadRepository leadRepository;
    private final ConstantLifecycleRepository constantLifecycleRepository;
    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final LeadLoaderService leadLoaderService;
    private final CampaignRepository campaignRepository;
    private final LeadService leadService;
    private final PlatformTransactionManager transactionManager;
    private final CompanyRepository companyRepository;


    public LoadOutreachCampaignService(ContactRepository contactRepository, LeadRepository leadRepository, ConstantLifecycleRepository constantLifecycleRepository, TaskRepository taskRepository, TaskService taskService, LeadLoaderService leadLoaderService, CampaignRepository campaignRepository, LeadService leadService, PlatformTransactionManager transactionManager, CompanyRepository companyRepository) {
        this.contactRepository = contactRepository;
        this.leadRepository = leadRepository;
        this.constantLifecycleRepository = constantLifecycleRepository;
        this.taskRepository = taskRepository;
        this.taskService = taskService;
        this.leadLoaderService = leadLoaderService;
        this.campaignRepository = campaignRepository;
        this.leadService = leadService;
        this.transactionManager = transactionManager;
        this.companyRepository = companyRepository;
    }

    public LoadOutreachCampaignResponseDTO loadOutreachCampaign(Long campaignId, String filePath) throws IOException {
        logger.info("Starting CSV import for campaign {} from: {}", campaignId, filePath);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found with ID: " + campaignId));

        logger.info("Campaign found: {}", campaign.getCampaignName());

        int[] leadsLoaded = new int[1]; // mutable counters
        int[] tasksCreated = new int[1];

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            logger.info("Header skipped: {}", br.readLine());
            String line;
            int lineNumber = 2;

            while ((line = br.readLine()) != null) {
                logger.info("Reading line {}: {}", lineNumber++, line);
                processCSVLine(line, campaign,leadsLoaded, tasksCreated);
            }

            logger.info("CSV import completed successfully for campaign {}", campaignId);
            logger.info("Total Leads Created: {}", leadsLoaded[0]);
            logger.info("Total Outreach Tasks Created: {}", tasksCreated[0]);
        }
        return new LoadOutreachCampaignResponseDTO(leadsLoaded[0], tasksCreated[0],"message");

    }

    private void processCSVLine(String line, Campaign campaign,int[] leadsLoaded, int[] tasksCreated) {
        String[] tokens = line.split(",");

        if (tokens.length < 12) {
            logger.warn("Skipping invalid row (less than 12 columns): {}", line);
            return;
        }

        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = removeQuotes(tokens[i]);
        }


        TransactionStatus tx = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            String companyName  = tokens[0];
            String firstName = tokens[1];
            String lastName = tokens[2];
            String email = tokens[3];
            if ("NA".equalsIgnoreCase(email.trim())) {
                email = "";
            }
            String linkedin = tokens[4];
            String phone = tokens[5];
            String city = tokens[6];
            String state = tokens[7];
            String country = tokens[8];
            String assignTo = tokens[9];
            String nextWorkDate = tokens[10];
            String designation = tokens[11];

            logger.info("Processing contact: {}", email);

            // Validate: skip if company name is blank
            if (companyName == null || companyName.trim().isEmpty()) {
                logger.warn("Skipping contact creation because company name is missing for email: {}", email);
                transactionManager.rollback(tx);
                return;
            }

            Company company =companyRepository.findByNameIgnoreCase(companyName)
                        .orElseGet(() -> {
                            Company newCompany = new Company();
                            newCompany.setName(companyName);
                            return companyRepository.save(newCompany);
                        });


            // Create or Update Contact
            //If the email exists, it updates the same Contact record.
            //If the email does not exist, it creates a new Contact record.
            Optional<Contact> existingContact;
            existingContact = contactRepository.findByCompanyAndName( company.getName(),firstName.trim(),lastName.trim());
            Contact contact = existingContact.orElse(new Contact());
            contact.setCompany(company);
            contact.setFirstName(firstName);
            contact.setLastName(lastName);
            contact.setEmailID(email);
            contact.setLinkedInUrl(linkedin);
            contact.setPhoneNo(phone);
            contact.setCity(city);
            contact.setState(state);
            contact.setCountry(country);
            contact.setDesignation(designation);

            if (contact.getContactId() != null) {
                contact.setLinkedInUrl(linkedin);
            }
            contact = contactRepository.save(contact);

            logger.info("Contact saved: {} with company {}", email,
                    company != null ? company.getName() : "N/A");

            logger.info("Contact created: {}", email);
            boolean newLeadCreated = false;

            // Create or Load Lead
            //Lead Name + Campaign ID is the unique key for Lead creation.
            //A Lead is considered unique within a specific campaign.
            Optional<Lead> existingLeadOpt = leadRepository.findByLeadNameAndCampaignIdWithFetch(companyName, campaign.getCampaignId());
            Lead lead = existingLeadOpt
                    .map(existingLead -> {
                        Lead loadedLead = leadLoaderService.loadLeadWithLifecycle(existingLead.getLeadId());
                        logger.debug("Attempting to load lead with lifecycle for ID: {}", loadedLead.getLeadId());
                        logger.info(" Lead already exists: {}", companyName);

                        logger.debug("Loaded existing lead with ID: {}", loadedLead.getLeadId());
                        return loadedLead;
                    })
                    .orElseGet(() -> {
                        Lead newLead = createNewLead(companyName, campaign);
                        logger.info(" Created new lead: {} in campaignId: {}", companyName, campaign.getCampaignId());
                        return newLead;
                    });

            if (!existingLeadOpt.isPresent()) {
                leadsLoaded[0]++;
            }

            // Get first lifecycle stage
            Lifecycle firstLifecycleStage = lead.getLeadlifecycleList().stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No lifecycle stages found for lead: " + companyName));

            logger.info("First lifecycle stage: {}", firstLifecycleStage.getLifecycleName());

            boolean taskCreated = createOutreachTasks(contact, firstLifecycleStage, assignTo, lead,nextWorkDate);

            if (taskCreated) {
                tasksCreated[0]++;
            }

            transactionManager.commit(tx);
        } catch (Exception ex) {
            transactionManager.rollback(tx);
            logger.error("Error processing row: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to process CSV row: " + line, ex);
        }
    }

    private Lead createNewLead(String company, Campaign campaign) {
        LeadDTO leadDTO = new LeadDTO();
        leadDTO.setLeadName(company);
        leadDTO.setLeadTitle(company);
        leadDTO.setCampaignId(campaign.getCampaignId());

        LeadDTO createdLeadDTO = leadService.createLead(leadDTO);
        return leadLoaderService.loadLeadWithLifecycle(createdLeadDTO.getLeadId());
    }

    private boolean createOutreachTaskIfNotExists(Contact contact, Lifecycle lifecycle, String assignTo,Lead lead, String nextWorkDateMillisStr) {
        Optional<EmailOutreachTask> existingTask = taskRepository.findByContactIdAndLifecycleId(
                contact.getContactId(), lifecycle.getLifecycleId());

        if (existingTask.isEmpty()) {
            EmailOutreachTask task = new EmailOutreachTask();
            task.setLifecycle(lifecycle);
            task.setRelatedContact(contact);

            List<ConstantLifecycle> outreachStatuses = constantLifecycleRepository.findByCycleId(5L);
            if (outreachStatuses.isEmpty()) {
                throw new RuntimeException("Email OUTREACH lifecycle stages not found");
            }

            task.setStatus(outreachStatuses.get(0).getCycleName());
            task.setTaskName("Connect with " + contact.getFirstName() + " " + contact.getLastName());
            task.setAssignTo(assignTo);// updates the DB column assign_to (who owns the task)

            if (nextWorkDateMillisStr != null && !nextWorkDateMillisStr.trim().isEmpty()) {
                String dateStr = nextWorkDateMillisStr.trim();
                List<String> patterns = Arrays.asList("dd-MM-yyyy", "dd/MM/yyyy","d-M-yyyy");

                boolean parsed = false;
                for (String pattern : patterns) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                        LocalDate localDate = LocalDate.parse(dateStr, formatter);
                        long millis = localDate.toEpochDay() * 24 * 60 * 60 * 1000L;
                        task.setDueDate(millis);
                        parsed = true;
                        break;
                    } catch (DateTimeParseException ignore) {

                    }
                }

                if (!parsed) {
                    logger.warn("Invalid date format for nextWorkDate: {}", dateStr);
                }
            }

            taskRepository.save(task);

            logger.info(" Created OutreachTask under lead: '{}', task name: '{}'", lead.getLeadName(), task.getTaskName());

            taskService.updateSendToAssigneeByTaskId(task.getTaskId(), true);//task send to user.
            return true;
        } else {
            logger.info(" OutreachTask already exists under lead: '{}', task name: '{}'", lead.getLeadName(), existingTask.get().getTaskName());
            return false;
        }
    }

    private boolean createOutreachTasks(Contact contact, Lifecycle lifecycle, String assignTo, Lead lead, String nextWorkDate) {
        boolean anyTaskCreated = false;

        // Email Outreach
        if (isValid(contact.getEmailID())) {
            boolean emailCreated = createSpecificOutreachTask(
                    contact,
                    lifecycle,
                    assignTo,
                    lead,
                    nextWorkDate,
                    5L, // cycleId for Email outreach
                    TaskType.EMAIL_OUTREACH_TASK,
                    "Email " + contact.getFirstName() + " " + contact.getLastName()
            );
            if (emailCreated) {
                anyTaskCreated = true;
            }
        }

        // Phone Outreach
        if (isValid(contact.getPhoneNo())) {
            boolean phoneCreated = createSpecificOutreachTask(
                    contact,
                    lifecycle,
                    assignTo,
                    lead,
                    nextWorkDate,
                    11L, // cycleId for Phone outreach
                    TaskType.PHONE_OUTREACH_TASK,
                    "Call " + contact.getFirstName() + " " + contact.getLastName()
            );
            if (phoneCreated) {
                anyTaskCreated = true;
            }
        }

        // LinkedIn Outreach
        if (isValid(contact.getLinkedInUrl())) {
            boolean linkedinCreated = createSpecificOutreachTask(
                    contact,
                    lifecycle,
                    assignTo,
                    lead,
                    nextWorkDate,
                    10L, // cycleId for LinkedIn outreach
                    TaskType.LINKEDIN_OUTREACH_TASK,
                    "LinkedIn " + contact.getFirstName() + " " + contact.getLastName()
            );
            if (linkedinCreated) {
                anyTaskCreated = true;
            }
        }

        return anyTaskCreated;
    }

    private boolean isValid(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean createSpecificOutreachTask(
            Contact contact, Lifecycle lifecycle, String assignTo, Lead lead,
            String nextWorkDateMillisStr, Long cycleId, String type, String taskName) {

        boolean taskExists = false;

        // Check if task already exists based on outreach type
        if (TaskType.EMAIL_OUTREACH_TASK.equalsIgnoreCase(type)) {
            taskExists = taskRepository
                    .findEmailOutreachTask(contact.getContactId(), lifecycle.getLifecycleId())
                    .isPresent();
        } else if (TaskType.PHONE_OUTREACH_TASK.equalsIgnoreCase(type)) {
            taskExists = taskRepository
                    .findPhoneOutreachTask(contact.getContactId(), lifecycle.getLifecycleId())
                    .isPresent();
        } else if (TaskType.LINKEDIN_OUTREACH_TASK.equalsIgnoreCase(type)) {
            taskExists = taskRepository
                    .findLinkedinOutreachTask(contact.getContactId(), lifecycle.getLifecycleId())
                    .isPresent();
        } else {
            logger.warn("Unsupported outreach type: {}", type);
            return false;
        }

        if (taskExists) {
            logger.info("{} task already exists for contact '{}'", type, contact.getEmailID());
            return false;
        }

        // Create task object
        Task task;
        if (TaskType.EMAIL_OUTREACH_TASK.equalsIgnoreCase(type)) {
            task = new EmailOutreachTask();
        } else if (TaskType.PHONE_OUTREACH_TASK.equalsIgnoreCase(type)) {
            task = new PhoneOutreachTask();
            task.setDuration(RadiusConstants.DEFAULT_WORK_DURATION);
        } else if (TaskType.LINKEDIN_OUTREACH_TASK.equalsIgnoreCase(type)) {
            task = new LinkedInOutreachTask();
        } else {
            throw new IllegalArgumentException("Unsupported outreach type: " + type);
        }

        // Set lifecycle & contact
        task.setLifecycle(lifecycle);
        if (task instanceof EmailOutreachTask emailTask) {
            emailTask.setRelatedContact(contact);
        } else if (task instanceof PhoneOutreachTask phoneTask) {
            phoneTask.setRelatedContact(contact);
        } else {
            LinkedInOutreachTask linkedinTask = (LinkedInOutreachTask) task;
            linkedinTask.setRelatedContact(contact);
        }

        // Get lifecycle stages
        List<ConstantLifecycle> statuses = constantLifecycleRepository.findByCycleId(cycleId);
        if (statuses.isEmpty()) {
            throw new RuntimeException(type + " lifecycle stages not found (cycleId " + cycleId + ")");
        }

        task.setStatus(statuses.get(0).getCycleName());
        task.setTaskName(taskName);
        task.setAssignTo(assignTo);

        // Parse and set due date if provided
        if (isValid(nextWorkDateMillisStr)) {
            setDueDate(task, nextWorkDateMillisStr);
        }

        // Save and notify
        taskRepository.save(task);
        taskService.updateSendToAssigneeByTaskId(task.getTaskId(), true);

        logger.info("Created {} task under lead '{}' for contact '{}'",
                type, lead.getLeadName(), contact.getEmailID());

        return true;
    }


    private void setDueDate(Task task, String dateStr) {
        List<String> patterns = Arrays.asList(
                "dd-MM-yyyy",   // e.g. 12-11-2025
                "dd/MM/yyyy",   // e.g. 12/11/2025
                "d-M-yyyy",     // e.g. 2-1-2025
                "d/M/yyyy",     // e.g. 2/1/2025
                "yyyy-MM-dd",   // ISO standard (e.g. 2025-11-12)
                "MM/dd/yyyy",   // US format (e.g. 11/12/2025)
                "M/d/yyyy",     // US short format (e.g. 1/2/2025)
                "dd MMM yyyy",  // e.g. 12 Nov 2025
                "d MMM yyyy",   // e.g. 2 Jan 2025
                "dd-MMM-yyyy",  // e.g. 12-Nov-2025
                "yyyy/MM/dd",   // e.g. 2025/11/12
                "yyyy.M.d",     // e.g. 2025.1.2
                "EEE, dd MMM yyyy", // e.g. Wed, 12 Nov 2025
                "dd.MM.yyyy"    // e.g. 12.11.2025
        );
        for (String pattern : patterns) {
            try {
                LocalDate localDate = LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern(pattern));
                long millis = localDate.toEpochDay() * 24 * 60 * 60 * 1000L + 60 * 60 * 1000L;//simply convert date into millis(1 am at night) without any zone
                task.setDueDate(millis);
                return;
            } catch (DateTimeParseException ignore) {}
        }
        logger.warn("Invalid date format for nextWorkDate: {}", dateStr);
    }

    public String removeQuotes(String token) {
        if (token == null) return null;

        token = token.trim();

        // Remove starting quotes
        if (!token.isEmpty() && (token.charAt(0) == '"' || token.charAt(0) == '\'')) {
            token = token.substring(1);
        }

        // Remove ending quotes
        if (!token.isEmpty() && (token.charAt(token.length() - 1) == '"' || token.charAt(token.length() - 1) == '\'')) {
            token = token.substring(0, token.length() - 1);
        }

        return token;
    }



}
