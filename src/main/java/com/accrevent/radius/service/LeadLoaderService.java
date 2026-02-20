package com.accrevent.radius.service;

import com.accrevent.radius.model.Lead;
import com.accrevent.radius.repository.LeadRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LeadLoaderService {

    @Autowired
    private LeadRepository leadRepository;

    @Transactional
    public Lead loadLeadWithLifecycle(Long leadId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found with ID: " + leadId));

        // Trigger lifecycle list initialization (solves LazyInitializationException)
        lead.getLeadlifecycleList().size();

        return lead;
    }
}
