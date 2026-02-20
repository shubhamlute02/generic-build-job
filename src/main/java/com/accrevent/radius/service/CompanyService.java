package com.accrevent.radius.service;

import com.accrevent.radius.dto.CompanyDTO;
import com.accrevent.radius.dto.CompanyResearchDTO;
import com.accrevent.radius.dto.ResearchTopicDTO;
import com.accrevent.radius.model.Company;
import com.accrevent.radius.model.CompanyResearch;
import com.accrevent.radius.model.ResearchTopic;
import com.accrevent.radius.repository.CompanyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public CompanyDTO createCompany(CompanyDTO companyDTO) {
        Company company = transformToCompany(companyDTO);
        return transformToCompanyDTO(companyRepository.save(company));
    }

    public CompanyDTO updateCompany(CompanyDTO companyDTO) {
        Company existingCompany = companyRepository.findById(companyDTO.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found with id: " + companyDTO.getCompanyId()));

        existingCompany.setName(companyDTO.getName());
        return transformToCompanyDTO(companyRepository.save(existingCompany));
    }

    public void deleteCompany(Long companyId) {
        companyRepository.deleteById(companyId);
    }

    public CompanyDTO getCompanyById(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company not found with id: " + companyId));
        return transformToCompanyDTO(company);
    }


    public List<CompanyDTO> getAllCompanies() {
        return companyRepository.findAll()
                .stream()
                .map(this::transformToCompanyDTO)
                .collect(Collectors.toList());
    }

    private Company transformToCompany(CompanyDTO companyDTO) {
        Company company = new Company();
        company.setCompanyId(companyDTO.getCompanyId());
        company.setName(companyDTO.getName());
        return company;
    }

//    private CompanyDTO transformToCompanyDTO(Company company) {
//        CompanyDTO companyDTO = new CompanyDTO();
//        companyDTO.setCompanyId(company.getCompanyId());
//        companyDTO.setName(company.getName());
//        return companyDTO;
//    }

    private CompanyDTO transformToCompanyDTO(Company company) {
        CompanyDTO companyDTO = new CompanyDTO();
        companyDTO.setCompanyId(company.getCompanyId());
        companyDTO.setName(company.getName());

        // Transform company research if it exists
        if (company.getCompanyResearch() != null) {
            companyDTO.setCompanyResearch(transformToCompanyResearchDTO(company.getCompanyResearch()));
        }

        return companyDTO;
    }

    private CompanyResearchDTO transformToCompanyResearchDTO(CompanyResearch companyResearch) {
        CompanyResearchDTO dto = new CompanyResearchDTO();
        dto.setId(companyResearch.getId());
        dto.setCompanyId(companyResearch.getCompany().getCompanyId());
        dto.setRevenue(companyResearch.getRevenue());
        dto.setEmployeeCount(companyResearch.getEmployeeCount());
        dto.setProductOrServices(companyResearch.getProductOrServices());
        dto.setHqCountry(companyResearch.getHqCountry());

        // Transform research topics if they exist
        if (companyResearch.getResearch() != null && !companyResearch.getResearch().isEmpty()) {
            dto.setResearch(companyResearch.getResearch().stream()
                    .map(this::transformToResearchTopicDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private ResearchTopicDTO transformToResearchTopicDTO(ResearchTopic researchTopic) {
        ResearchTopicDTO dto = new ResearchTopicDTO();
        dto.setId(researchTopic.getId());
        dto.setTopic(researchTopic.getTopic());
        dto.setDescription(researchTopic.getDescription());
        return dto;
    }


}
