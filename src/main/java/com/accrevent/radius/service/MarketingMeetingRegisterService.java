package com.accrevent.radius.service;

import com.accrevent.radius.model.*;
import com.accrevent.radius.repository.MarketingMeetingRegisterRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MarketingMeetingRegisterService {

    private final MarketingMeetingRegisterRepository marketingMeetingRegisterRepository;

    public MarketingMeetingRegisterService(MarketingMeetingRegisterRepository marketingMeetingRegisterRepository) {
        this.marketingMeetingRegisterRepository = marketingMeetingRegisterRepository;
    }

    private long toEpochMillis(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    private LocalDate toLocalDate(Long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate();
    }


    @Transactional
    public void registerMeeting(Task task) {

        Campaign campaign = null;
        String leadTitle = null;
        String contactName = null;
        String contactDesignation = null;


        // derive campaign
        if (task.getLifecycle() != null) {
            if (task.getLifecycle().getCampaign() != null) {
                campaign = task.getLifecycle().getCampaign();
            } else if (task.getLifecycle().getLead() != null) {
                Lead lead = task.getLifecycle().getLead();
                leadTitle = lead.getLeadTitle();

                if (lead.getCampaign() != null) {
                    campaign = lead.getCampaign();
                }
            }
        }

        // For outreach task, extract contact info
        if (task instanceof PhoneOutreachTask) {
            PhoneOutreachTask phoneOutreachTask = (PhoneOutreachTask) task;
            if (phoneOutreachTask.getRelatedContact() != null) {
                Contact contact = phoneOutreachTask.getRelatedContact();
                contactName = contact.getFirstName()+" "+contact.getLastName();
                contactDesignation = contact.getDesignation();

            }
        }else if (task instanceof LinkedInOutreachTask){
            LinkedInOutreachTask linkedInOutreachTask = (LinkedInOutreachTask) task;
            if (linkedInOutreachTask.getRelatedContact()!= null){
                Contact contact = linkedInOutreachTask.getRelatedContact();
                contactName = contact.getFirstName()+" "+contact.getLastName();
                contactDesignation = contact.getDesignation();
            }

        } else if(task instanceof EmailOutreachTask) {
            EmailOutreachTask emailOutreachTask = (EmailOutreachTask) task;
            if (emailOutreachTask.getRelatedContact()!= null){
                Contact contact = emailOutreachTask.getRelatedContact();
                contactName = contact.getFirstName()+" "+contact.getLastName();
                contactDesignation = contact.getDesignation();
            }
        }

        // Create entry
        MarketingMeetingRegister register = new MarketingMeetingRegister();
        register.setCampaign(campaign);
        register.setCreatedDate(System.currentTimeMillis());
        register.setLeadTitle(leadTitle);
        register.setContactName(contactName);
        register.setContactDesignation(contactDesignation);


        marketingMeetingRegisterRepository.save(register);
    }

    public List<Map<String, Object>> getMarketingMeetingRegisterReport() {
//        List<MarketingMeetingRegister> registers =
//                marketingMeetingRegisterRepository.findByCampaign_CampaignId(campaignId);

        List<MarketingMeetingRegister> registers = marketingMeetingRegisterRepository.findAll();

        // Group by LocalDate
        Map<LocalDate, List<MarketingMeetingRegister>> groupedByDate = registers.stream()
                .collect(Collectors.groupingBy(r -> toLocalDate(r.getCreatedDate()),
                        LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> report = new ArrayList<>();

        for (Map.Entry<LocalDate, List<MarketingMeetingRegister>> entry : groupedByDate.entrySet()) {
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("count", entry.getValue().size());
            group.put("createdDate", toEpochMillis(entry.getKey())); // same UTC logic

            List<Map<String, Object>> records = entry.getValue().stream().map(r -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("createdDate", r.getCreatedDate());
                row.put("leadTitle", r.getLeadTitle());
                row.put("contactName", r.getContactName());
                row.put("contactDesignation", r.getContactDesignation());
                return row;
            }).collect(Collectors.toList());

            group.put("records", records);
            report.add(group);
        }

        return report;
    }

}
