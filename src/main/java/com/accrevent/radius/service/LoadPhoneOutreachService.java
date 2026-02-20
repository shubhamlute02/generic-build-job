package com.accrevent.radius.service;

import com.accrevent.radius.dto.LoadOutreachCampaignResponseDTO;
import com.accrevent.radius.model.*;
import com.accrevent.radius.repository.*;
import com.accrevent.radius.util.RadiusConstants;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class LoadPhoneOutreachService {

    private static final Logger logger = LoggerFactory.getLogger(LoadPhoneOutreachService.class);

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

    public LoadPhoneOutreachService(ContactRepository contactRepository,
                                    LeadRepository leadRepository,
                                    ConstantLifecycleRepository constantLifecycleRepository,
                                    TaskRepository taskRepository,
                                    TaskService taskService,
                                    LeadLoaderService leadLoaderService,
                                    CampaignRepository campaignRepository,
                                    LeadService leadService,
                                    PlatformTransactionManager transactionManager,
                                    CompanyRepository companyRepository) {
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

    public LoadOutreachCampaignResponseDTO loadPhoneOutreach(Long campaignId, String filePath) throws IOException {
        logger.info("Starting PHONE CSV import for campaign {} from: {}", campaignId, filePath);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found with ID: " + campaignId));

        int[] leadsLoaded = new int[1]; // kept for DTO compatibility; remains zero because we don't create leads here
        int[] tasksCreated = new int[1];

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            logger.info("Header skipped: {}", br.readLine());
            String line;
            int lineNumber = 2;
            while ((line = br.readLine()) != null) {
                logger.info("Reading line {}: {}", lineNumber++, line);
                processCSVLine(line, campaign, leadsLoaded, tasksCreated);
            }
            logger.info("PHONE CSV import completed for campaign {}. Leads created: {}, Phone tasks created: {}",
                    campaignId, leadsLoaded[0], tasksCreated[0]);
        }

        return new LoadOutreachCampaignResponseDTO(leadsLoaded[0], tasksCreated[0], "Phone import complete");
    }

    private void processCSVLine(String line, Campaign campaign, int[] leadsLoaded, int[] tasksCreated) {
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

            logger.info("Processing contact for company '{}', email '{}'", companyName, email);

            // Validate company name
            if (companyName == null || companyName.trim().isEmpty()) {
                logger.warn("Skipping contact update because company name is missing for email: {}", email);
                transactionManager.rollback(tx);
                return;
            }

            Optional<Company> company = companyRepository.findByNameIgnoreCase(companyName);

            if(company.isEmpty()) {
                logger.info("Company not found: {} — skipping row.", companyName);
                transactionManager.rollback(tx);
                return;
            }

            // Create or update Contact
            Optional<Contact> existingContact = contactRepository.findByCompanyAndName(company.get().getName(), firstName.trim(), lastName.trim());

            if(existingContact.isEmpty())
            {
                logger.info("Contact not found: {} {} for company {} — skipping row.", firstName.trim(), lastName.trim(), companyName);
                transactionManager.rollback(tx);
                return;
            }

            Contact contact = existingContact.get();
            contact.setCompany(company.get());
            contact.setFirstName(firstName);
            contact.setLastName(lastName);
            contact.setEmailID(email);
            contact.setLinkedInUrl(linkedin);
            contact.setPhoneNo(phone);
            contact.setCity(city);
            contact.setState(state);
            contact.setCountry(country);
            contact.setDesignation(designation);

            contact = contactRepository.save(contact);

            logger.info("Contact saved/updated: {} / company: {}", email, company.get().getName());

            //CHANGE =  Do NOT create a new Lead. Only update existing leads
            Optional<Lead> existingLeadOpt = leadRepository.findByLeadNameAndCampaignIdWithFetch(companyName, campaign.getCampaignId());
            if (existingLeadOpt.isEmpty()) {
                // If no lead exists we skip creating a lead
                logger.info("No existing lead found for company '{}' in campaign {} — skipping phone task creation.", companyName, campaign.getCampaignId());
                transactionManager.rollback(tx);
                return;
            }

            // Load the existing lead with lifecycle
            Lead lead = leadLoaderService.loadLeadWithLifecycle(existingLeadOpt.get().getLeadId());
            Lifecycle firstLifecycleStage = lead.getLeadlifecycleList().stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No lifecycle stages found for lead: " + companyName));

            // Create phone outreach task
            boolean taskCreated = false;
            if (isValid(contact.getPhoneNo())) {
                taskCreated = createPhoneOutreachTask(contact, firstLifecycleStage, assignTo, lead, nextWorkDate);
            } else {
                logger.info("Phone not present/invalid for contact {} — skipping phone task.", contact.getEmailID());
            }

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

    private boolean createPhoneOutreachTask(Contact contact, Lifecycle lifecycle, String assignTo, Lead lead, String nextWorkDate) {
        // Check if phone task already exists for this contact and lifecycle
        boolean exists = taskRepository.findPhoneOutreachTask(contact.getContactId(), lifecycle.getLifecycleId()).isPresent();
        if (exists) {
            logger.info("Phone outreach task already exists for contact '{}'", contact.getEmailID());
            return false;
        }

        PhoneOutreachTask task = new PhoneOutreachTask();
        task.setLifecycle(lifecycle);
        task.setRelatedContact(contact);
        task.setDuration(RadiusConstants.DEFAULT_WORK_DURATION);
        logger.info("phone outreach task created.");

        // find phone lifecycle statuses (assumes 11L)
        List<ConstantLifecycle> statuses = constantLifecycleRepository.findByCycleId(11L);
        if (statuses.isEmpty()) {
            throw new RuntimeException("Phone OUTREACH lifecycle stages not found (cycleId 11)");
        }

        task.setStatus(statuses.get(0).getCycleName());
        task.setTaskName("Call " + contact.getFirstName() + " " + contact.getLastName());
        task.setAssignTo(assignTo);

        if (isValid(nextWorkDate)) {
            setDueDate(task, nextWorkDate);
        }

        taskRepository.save(task);
        taskService.updateSendToAssigneeByTaskId(task.getTaskId(), true);

        logger.info("Created PHONE task under lead '{}' for contact '{}'", lead.getLeadName(), contact.getEmailID());
        return true;
    }

    private boolean isValid(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void setDueDate(Task task, String dateStr) {
        List<String> patterns = Arrays.asList(
                "dd-MM-yyyy", "dd/MM/yyyy", "d-M-yyyy", "d/M/yyyy",
                "yyyy-MM-dd", "MM/dd/yyyy", "M/d/yyyy",
                "dd MMM yyyy", "d MMM yyyy", "dd-MMM-yyyy",
                "yyyy/MM/dd", "yyyy.M.d", "EEE, dd MMM yyyy", "dd.MM.yyyy"
        );
        for (String pattern : patterns) {
            try {
                LocalDate localDate = LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern(pattern));
                long millis = localDate.toEpochDay() * 24 * 60 * 60 * 1000L + 60 * 60 * 1000L;
                task.setDueDate(millis);
                return;
            } catch (DateTimeParseException ignore) {}
        }
        logger.warn("Invalid date format for nextWorkDate: {}", dateStr);
    }

    public String removeQuotes(String token) {
        if (token == null) return null;

        token = token.trim();
        if (!token.isEmpty() && (token.charAt(0) == '"' || token.charAt(0) == '\'')) {
            token = token.substring(1);
        }
        if (!token.isEmpty() && (token.charAt(token.length() - 1) == '"' || token.charAt(token.length() - 1) == '\'')) {
            token = token.substring(0, token.length() - 1);
        }
        return token;
    }
}
