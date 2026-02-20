package com.accrevent.radius.service;

import com.accrevent.radius.dto.ResearchTopicDTO;
import com.accrevent.radius.model.CompanyResearch;
import com.accrevent.radius.model.ResearchTopic;
import com.accrevent.radius.repository.CompanyResearchRepository;
import com.accrevent.radius.repository.ResearchTopicRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResearchTopicService {
    @Autowired
    private CompanyResearchRepository companyResearchRepository;


    @Autowired
    private ResearchTopicRepository researchTopicRepository;

    public ResearchTopicDTO createResearchTopic(Long companyResearchId, ResearchTopicDTO dto) {

        CompanyResearch companyResearch = companyResearchRepository.findById(companyResearchId)
                .orElseThrow(() -> new RuntimeException("CompanyResearch not found"));

        ResearchTopic topic = new ResearchTopic();
        topic.setTopic(dto.getTopic());
        topic.setDescription(dto.getDescription());
        topic.setCompanyResearch(companyResearch);

        ResearchTopic saved = researchTopicRepository.save(topic);

        ResearchTopicDTO result = new ResearchTopicDTO();
        result.setId(saved.getId());
        result.setTopic(saved.getTopic());
        result.setDescription(saved.getDescription());

        return result;
    }

    @Transactional
    public ResearchTopicDTO updateResearchTopic(ResearchTopicDTO dto) {
        ResearchTopic topic = researchTopicRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Research topic not found"));

        topic.setTopic(dto.getTopic());
        topic.setDescription(dto.getDescription());

        ResearchTopic saved = researchTopicRepository.save(topic);

        ResearchTopicDTO result = new ResearchTopicDTO();
        result.setId(saved.getId());
        result.setTopic(saved.getTopic());
        result.setDescription(saved.getDescription());
        return result;
    }

    @Transactional
    public void deleteResearchTopic(Long topicId) {
        ResearchTopic topic = researchTopicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Research topic not found"));
        researchTopicRepository.delete(topic);
    }

}
