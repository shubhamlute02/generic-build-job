package com.accrevent.radius.repository;


import com.accrevent.radius.model.Company;
import com.accrevent.radius.model.CompanyResearch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyResearchRepository extends JpaRepository<CompanyResearch, Long> {
        Optional<CompanyResearch> findByCompanyCompanyId(Long companyId);

    Optional<CompanyResearch> findByCompany(Company company);
}

