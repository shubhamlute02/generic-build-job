//package com.accrevent.radius.load;
//
//import com.accrevent.radius.model.Contact;
//import com.accrevent.radius.model.Lead;
//import com.accrevent.radius.model.OutreachTask;
//import com.accrevent.radius.repository.ContactRepository;
//import com.accrevent.radius.repository.LeadRepository;
//import com.accrevent.radius.repository.LifecycleRepository;
//import com.accrevent.radius.repository.TaskRepository;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.context.ConfigurableApplicationContext;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//
//@SpringBootApplication(scanBasePackages = "com.accrevent.radius")
//public class LoadOutreachTasks {
//
//    public static void main(String[] args) {
//        if (args.length == 0) {
//            System.err.println(" Please provide the CSV file path as an argument.");
//            return;
//        }
//
//        String filePath = args[0];
//        System.out.println(" Starting CSV import from: " + filePath);
//
//        // Start Spring and get the ApplicationContext
//        ConfigurableApplicationContext context = SpringApplication.run(LoadOutreachTasks.class, args);
//
//        // Get beans (repositories) from Spring context
//        ContactRepository contactRepo = context.getBean(ContactRepository.class);
//        LeadRepository leadRepo = context.getBean(LeadRepository.class);
//        TaskRepository taskRepo = context.getBean(TaskRepository.class);
//
//        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
//            br.readLine(); // skip header line
//            String line;
//
//            while ((line = br.readLine()) != null) {
//                String[] tokens = line.split(",");
//                if (tokens.length < 10) {
//                    System.out.println(" Skipping invalid row: " + line);
//                    continue;
//                }
//
//                String company = tokens[0].trim();
//                String firstName = tokens[1].trim();
//                String lastName = tokens[2].trim();
//                String email = tokens[3].trim();
//                String linkedin = tokens[4].trim();
//                String phone = tokens[5].trim();
//                String city = tokens[6].trim();
//                String state = tokens[7].trim();
//                String country = tokens[8].trim();
//                String assignTo = tokens[9].trim();
//
//                // 1. Create/update Contact
//                Contact contact = contactRepo.findByEmailID(email).orElse(new Contact());
//                contact.setCompany(company);
//                contact.setFirstName(firstName);
//                contact.setLastName(lastName);
//                contact.setEmailID(email);
//                contact.setLinkedInUrl(linkedin);
//                contact.setPhoneNo(phone);
//                contact.setCity(city);
//                contact.setState(state);
//                contact.setCountry(country);
//                contact = contactRepo.save(contact);
//
//                // 2. Create Lead
//                Lead lead = new Lead();
//                lead.setLeadTitle(company);
//                lead.setLeadName(company);
//                leadRepo.save(lead);
//
//                // 3. Create OutreachTask
//                OutreachTask task = new OutreachTask();
//                task.setRelatedContact(contact);
//                task.setTaskName("Connect with " + firstName + " " + lastName);
//                task.setAssignTo(assignTo);
//                taskRepo.save(task);
//            }
//
//            System.out.println(" CSV import completed successfully.");
//
//        } catch (IOException e) {
//            System.err.println(" Failed to load CSV: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        context.close(); // optional: close Spring context
//    }
//}



package com.accrevent.radius.load;


import com.accrevent.radius.dto.LeadDTO;
import com.accrevent.radius.model.*;
import com.accrevent.radius.repository.*;
import com.accrevent.radius.service.LeadLoaderService;
import com.accrevent.radius.service.LeadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication(scanBasePackages = "com.accrevent.radius")
public class LoadOutreachTasks {

    private static final Logger logger = LoggerFactory.getLogger(LoadOutreachTasks.class);

//    public static void main(String[] args) {
//        if (args.length < 2) {
//            logger.error("Missing arguments: Expected <campaignId> <csvFilePath>. Received {} argument(s).", args.length);
//            logger.error("Usage: java com.accrevent.radius.load.LoadOutreachTasks <campaignId> <csvFilePath>");
//            return;
//        }
//
//        Long campaignId = Long.parseLong(args[0]);
//        String csvFilePath = args[1];
//        logger.info(" Campaign ID received: {}", campaignId);
//
//        ConfigurableApplicationContext context = SpringApplication.run(LoadOutreachTasks.class, args);
//
//        ContactRepository contactRepo = context.getBean(ContactRepository.class);
//        LeadRepository leadRepo = context.getBean(LeadRepository.class);
//        ConstantLifecycleRepository constantLifecycleRepository = context.getBean(ConstantLifecycleRepository.class);
//        TaskRepository taskRepo = context.getBean(TaskRepository.class);
//        LeadLoaderService leadLoaderService = context.getBean(LeadLoaderService.class);
//        CampaignRepository campaignRepo = context.getBean(CampaignRepository.class);
//        LeadService leadService = context.getBean(LeadService.class);
//
//        //for avoiding LazyInitializationException
//        // Manually manage transaction
//        PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
//
//        Campaign campaign = campaignRepo.findById(campaignId).orElse(null);
//        if (campaign == null) {
//            logger.error("Campaign ID not found: {}", campaignId);
//            return;
//        }
//
//        logger.info("Campaign found: {}", campaign.getCampaignName());
//        logger.info("Started reading CSV");
//
//        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
//            logger.info("Header skipped: {}", br.readLine());
//            String line;
//            int lineNumber = 2;
//
//            while ((line = br.readLine()) != null) {
//                logger.info("Reading line {}: {}", lineNumber++, line);
//                StringTokenizer tokenizer = new StringTokenizer(line, ",");
//                List<String> tokens = new ArrayList<>();
//
//                while (tokenizer.hasMoreTokens()) {
//                    tokens.add(tokenizer.nextToken().trim());
//                }
//
//                if (tokens.size() < 10) {
//                    logger.warn(" Skipping invalid row (less than 10 columns): {}", line);
//                    continue;
//                }
//
//                // Start transaction manually
//                TransactionStatus tx = transactionManager.getTransaction(new DefaultTransactionDefinition());
//                try {
//
//                    logger.info(" Processing CSV row - Company: '{}', Email: '{}'", tokens.get(0), tokens.get(3));String company = tokens.get(0);
//
//                    String firstName = tokens.get(1);
//                    String lastName = tokens.get(2);
//                    String email = tokens.get(3);
//                    String linkedin = tokens.get(4);
//                    String phone = tokens.get(5);
//                    String city = tokens.get(6);
//                    String state = tokens.get(7);
//                    String country = tokens.get(8);
//                    String assignTo = tokens.get(9);
//
//                logger.info("Processing contact: {}", email);
//
//                // Create or Update Contact
//                Contact contact = contactRepo.findByEmailID(email).orElse(new Contact());
//                contact.setCompany(company);
//                contact.setFirstName(firstName);
//                contact.setLastName(lastName);
//                contact.setEmailID(email);
//                contact.setLinkedInUrl(linkedin);
//                contact.setPhoneNo(phone);
//                contact.setCity(city);
//                contact.setState(state);
//                contact.setCountry(country);
//                contact = contactRepo.save(contact);
//
//                    logger.info("contact Created: {}", email);
//
//
//               //  1. Create Lead using LeadService (handles lifecycle creation internally)
//
//                    Optional<Lead> existingLeadOpt = leadRepo.findByLeadNameAndCampaignIdWithFetch(company, campaign.getCampaignId());
//
//                    Lead lead;
//                    if (existingLeadOpt.isPresent()) {
//                        lead = leadLoaderService.loadLeadWithLifecycle(existingLeadOpt.get().getLeadId());
//                        logger.debug("Attempting to load lead with lifecycle for ID: {}", lead.getLeadId());
//                        logger.info(" Lead already exists: {}", company);
//
//
//
//                    } else {
//                        // Create Lead using LeadService (handles lifecycle creation and setup)
//                        LeadDTO leadDTO = new LeadDTO();
//                        leadDTO.setLeadName(company);
//                        leadDTO.setLeadTitle(company);
//                        leadDTO.setCampaignId(campaign.getCampaignId());
//
//                        LeadDTO createdLeadDTO = leadService.createLead(leadDTO);
//                        lead = leadLoaderService.loadLeadWithLifecycle(createdLeadDTO.getLeadId());
//
//                        logger.info(" Created new lead: {} in campaignId: {}", company, campaign.getCampaignId());
//
//                    }
//
//                    //  3. Get first lifecycle stage (e.g., "identified")
//                Lifecycle firstLifecycleStage = lead.getLeadlifecycleList().stream()
//                        .findFirst()
//                        .orElseThrow(() -> new RuntimeException("No lifecycle stages found for lead: " + company));
//
//                    logger.info(" First lifecycle stage: {}", firstLifecycleStage.getLifecycleName());
//
//
//                    // 4. Check if OutreachTask already exists for this Contact + Lead
//                    Optional<OutreachTask> existingTask = taskRepo.findByContactIdAndLifecycleId(contact.getContactId(), firstLifecycleStage.getLifecycleId());
//
//                //  5. Create task if not already existing
//                if (existingTask.isEmpty()) {
//                    OutreachTask task = new OutreachTask();
//                    //connecting task to lead by using lead lifecycle
//                    task.setLifecycle(firstLifecycleStage);
//                    System.out.println(firstLifecycleStage+"firstLifecycleStage");
//                    task.setRelatedContact(contact);
//
//                    // Get first outreach status from ConstantLifecycle (e.g., "not started")
//                    List<ConstantLifecycle> outreachStatuses = constantLifecycleRepository.findByCycleId(5L);
//
//                    if (outreachStatuses.isEmpty()) {
//                        throw new RuntimeException("OUTREACH lifecycle stages not found");
//                    }
//
//                    ConstantLifecycle firstOutreachStatus = outreachStatuses.get(0); //  Gets the first in list
//
//                    task.setStatus(firstOutreachStatus.getCycleName()); // e.g., "not started"
//                    task.setTaskName("Connect with " + contact.getFirstName() + " " + contact.getLastName());
//                    task.setAssignTo(assignTo);
//                    taskRepo.save(task);
//
//                    logger.info(" Created OutreachTask under lead: '{}', task name: '{}'", lead.getLeadName(), task.getTaskName());
//
//                } else {
//                    logger.info(" OutreachTask already exists under lead: '{}', task name: '{}'", lead.getLeadName(), existingTask.get().getTaskName());
//
//                }
//                    transactionManager.commit(tx);
//                } catch (Exception ex) {
//                    transactionManager.rollback(tx); //  rollback on error
//                    logger.error(" Error processing row: {}", ex.getMessage(), ex);
//                    ex.printStackTrace();
//                }
//
//            }
//            logger.info("CSV import completed successfully.");
//        }
//
//        catch (IOException e) {
//            logger.error(" Error reading CSV: {}", e.getMessage(), e);
//            e.printStackTrace();
//        }
//    }
}
