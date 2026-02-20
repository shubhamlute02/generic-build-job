package com.accrevent.radius.repository;

import com.accrevent.radius.model.Company;
import com.accrevent.radius.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    Optional<Contact> findByEmailID(String emailID);
    boolean existsByEmailIDAndCompany(String emailID, Company company);

    List<Contact> findByCompany(Company company);

    @Query("SELECT c FROM Contact c WHERE " +
            "LOWER(c.company.name) = LOWER(:companyName) AND " +
            "LOWER(c.firstName) = LOWER(:firstName) AND " +
            "LOWER(c.lastName) = LOWER(:lastName)")
    Optional<Contact> findByCompanyAndName(String companyName, String firstName, String lastName);


}
