package com.accrevent.radius.service;

import com.accrevent.radius.dto.CompanyResearchDTO;
import com.accrevent.radius.dto.ResearchTopicDTO;
import com.accrevent.radius.model.Company;
import com.accrevent.radius.model.CompanyResearch;
import com.accrevent.radius.model.ResearchTopic;
import com.accrevent.radius.repository.CompanyRepository;
import com.accrevent.radius.repository.CompanyResearchRepository;
import com.accrevent.radius.repository.ResearchTopicRepository;
import com.accrevent.radius.util.CompanyResearchConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;
import java.util.stream.Collectors;

@Service
public class CompanyResearchService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyResearchService.class);


    private final CompanyResearchConstants companyResearchConstants;
    private final CompanyResearchRepository companyResearchRepository;
    private final CompanyRepository companyRepository;
    private final ResearchTopicRepository researchTopicRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public CompanyResearchService(CompanyResearchConstants companyResearchConstants,
                                  CompanyResearchRepository companyResearchRepository,
                                  CompanyRepository companyRepository, ResearchTopicRepository researchTopicRepository) {
        this.companyResearchConstants = companyResearchConstants;
        this.companyResearchRepository = companyResearchRepository;
        this.companyRepository = companyRepository;
        this.researchTopicRepository = researchTopicRepository;
    }

    public List<String> getCompanyRevenue() {
        return new ArrayList<>(CompanyResearchConstants.REVENUE);
    }

    public List<String> getCompanyEmployeeCount() {
        return new ArrayList<>(CompanyResearchConstants.EMPLOYEE_COUNT);
    }

    public CompanyResearchDTO getCompanyResearchDetails(Long companyId) {
        Optional<CompanyResearch> researchOptional = companyResearchRepository.findByCompanyCompanyId(companyId);

        if (researchOptional.isPresent()) {
            return transformToDTO(researchOptional.get());
        } else {
            CompanyResearchDTO dto = new CompanyResearchDTO();
            dto.setCompanyId(companyId);
            return dto;
        }
    }

    private CompanyResearchDTO transformToDTO(CompanyResearch entity) {
        CompanyResearchDTO dto = new CompanyResearchDTO();

        dto.setId(entity.getId());

        if (entity.getCompany() != null) {
            dto.setCompanyId(entity.getCompany().getCompanyId());
        }

        dto.setRevenue(entity.getRevenue());
        dto.setEmployeeCount(entity.getEmployeeCount());
        dto.setProductOrServices(entity.getProductOrServices());
        dto.setHqCountry(entity.getHqCountry());

        List<ResearchTopicDTO> topics = entity.getResearch().stream().map(t -> {
            ResearchTopicDTO rt = new ResearchTopicDTO();
            rt.setId(t.getId());
            rt.setTopic(t.getTopic());
            rt.setDescription(t.getDescription());
            return rt;
        }).toList();

        dto.setResearch(topics);
        return dto;
    }

    public void addResearchTopic(CompanyResearch research, ResearchTopic topic) {
        if (research.getResearch() == null) {
            research.setResearch(new ArrayList<>());
        }
        topic.setCompanyResearch(research);
        research.getResearch().add(topic);
    }

    // Remove research topic helper
    public void removeResearchTopic(CompanyResearch research, ResearchTopic topic) {
        if (research.getResearch() != null) {
            research.getResearch().remove(topic);
            topic.setCompanyResearch(null);
        }
    }

@Transactional
public CompanyResearchDTO addCompanyResearch(CompanyResearchDTO dto) {
    Company company = companyRepository.findById(dto.getCompanyId())
            .orElseThrow(() -> new RuntimeException("Company not found"));

    CompanyResearch research = companyResearchRepository.findByCompany(company)
            .orElse(new CompanyResearch());

    research.setCompany(company);
    research.setRevenue(dto.getRevenue());
    research.setEmployeeCount(dto.getEmployeeCount());
    research.setProductOrServices(dto.getProductOrServices());
    research.setHqCountry(dto.getHqCountry());

    // Map of existing topics
    Map<Long, ResearchTopic> existingTopics = research.getResearch() != null
            ? research.getResearch().stream()
            .collect(Collectors.toMap(ResearchTopic::getId, t -> t))
            : new HashMap<>();

    if (dto.getResearch() != null) {
        for (ResearchTopicDTO topicDTO : dto.getResearch()) {
            if (topicDTO.getId() != null && existingTopics.containsKey(topicDTO.getId())) {
                // Update existing topic
                ResearchTopic existing = existingTopics.get(topicDTO.getId());
                existing.setTopic(topicDTO.getTopic());
                existing.setDescription(topicDTO.getDescription());
            } else {
                // Add new topic
                ResearchTopic topic = new ResearchTopic();
                topic.setTopic(topicDTO.getTopic());
                topic.setDescription(topicDTO.getDescription());
                topic.setCompanyResearch(research);
//                research.addResearchTopic(topic);
                addResearchTopic(research, topic);
            }
        }
    }

    CompanyResearch saved = companyResearchRepository.save(research);

    // Map entity back to DTO
    CompanyResearchDTO result = new CompanyResearchDTO();
    result.setId(saved.getId());
    result.setCompanyId(saved.getCompany().getCompanyId());
    result.setRevenue(saved.getRevenue());
    result.setEmployeeCount(saved.getEmployeeCount());
    result.setProductOrServices(saved.getProductOrServices());
    result.setHqCountry(saved.getHqCountry());

    if (saved.getResearch() != null) {
        result.setResearch(
                saved.getResearch().stream()
                        .map(r -> new ResearchTopicDTO(r.getId(), r.getTopic(), r.getDescription()))
                        .toList()
        );
    }

    return result;
}


    @Transactional
    public CompanyResearchDTO updateCompanyResearch(CompanyResearchDTO dto) {
        CompanyResearch research = companyResearchRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Company research not found"));

        research.setRevenue(dto.getRevenue());
        research.setEmployeeCount(dto.getEmployeeCount());
        research.setProductOrServices(dto.getProductOrServices());
        research.setHqCountry(dto.getHqCountry());

        Map<Long, ResearchTopic> existingTopics = research.getResearch().stream()
                .collect(Collectors.toMap(ResearchTopic::getId, t -> t));

        if (dto.getResearch() != null) {
            for (ResearchTopicDTO r : dto.getResearch()) {
                if (r.getId() != null && existingTopics.containsKey(r.getId())) {
                    ResearchTopic existing = existingTopics.get(r.getId());
                    existing.setTopic(r.getTopic());
                    existing.setDescription(r.getDescription());
                } else {
                    ResearchTopic newTopic = new ResearchTopic();
                    newTopic.setTopic(r.getTopic());
                    newTopic.setDescription(r.getDescription());
                    newTopic.setCompanyResearch(research);
                    research.getResearch().add(newTopic);
                }
            }
        }

        companyResearchRepository.save(research);

        CompanyResearchDTO response = new CompanyResearchDTO();
        response.setId(dto.getId());
        response.setCompanyId(dto.getCompanyId());
        response.setRevenue(dto.getRevenue());
        response.setEmployeeCount(dto.getEmployeeCount());
        response.setProductOrServices(dto.getProductOrServices());
        response.setHqCountry(dto.getHqCountry());
        response.setResearch(dto.getResearch());

        return response;
    }


    @Transactional
    public void deleteCompanyResearch(Long companyResearchId) {
        CompanyResearch research = companyResearchRepository.findById(companyResearchId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Company Research not found with id: " + companyResearchId
                ));

        Company company = research.getCompany();
        if (company != null) {
            company.setCompanyResearch(null);
        }


        companyResearchRepository.delete(research);
    }

}