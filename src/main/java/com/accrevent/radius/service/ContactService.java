package com.accrevent.radius.service;

import com.accrevent.radius.dto.CompanyDTO;
import com.accrevent.radius.dto.ContactDTO;
import com.accrevent.radius.model.Company;
import com.accrevent.radius.model.Contact;
import com.accrevent.radius.repository.CompanyRepository;
import com.accrevent.radius.repository.ContactRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.List;

@Service
public class ContactService {

    private  final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;

    public ContactService(ContactRepository contactRepository, CompanyRepository companyRepository) {
        this.contactRepository = contactRepository;
        this.companyRepository = companyRepository;
    }
    public ContactDTO addContact(ContactDTO dto) {
        if (dto.getCompany() == null || dto.getCompany().getCompanyId() == null) {
            throw new IllegalArgumentException("Company ID is required to create contact");
        }

        Company company = companyRepository.findById(dto.getCompany().getCompanyId())
                .orElseThrow(() -> new NoSuchElementException("Company not found"));

        // Check for duplicate email within the same company
        boolean exists = contactRepository.existsByEmailIDAndCompany(dto.getEmailID(), company);
        if (exists) {
            throw new IllegalArgumentException("A contact with this email already exists in the company");
        }

        // Transform DTO → Entity
        Contact contact = transformToContact(dto);
        contact.setCompany(company);

        // Save and return DTO
        return transformToContactDTO(contactRepository.save(contact));
    }

    public List<ContactDTO> getContactsByCompanyId(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NoSuchElementException("Company not found with id: " + companyId));

        List<Contact> contacts = contactRepository.findByCompany(company);
        return contacts.stream()
                .map(this::transformToContactDTO)
                .collect(Collectors.toList());
    }

    public ContactDTO getContactById(Long contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new NoSuchElementException("Contact not found with id: " + contactId));
        return transformToContactDTO(contact);
    }


    //  Update Contact
    public ContactDTO updateContact(Long id, ContactDTO updatedDto) {
        Contact existing = contactRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Contact not found: " + id));

        existing.setFirstName(updatedDto.getFirstName());
        existing.setLastName(updatedDto.getLastName());
        existing.setEmailID(updatedDto.getEmailID());
        existing.setLinkedInUrl(updatedDto.getLinkedInUrl());
        existing.setPhoneNo(updatedDto.getPhoneNo());
        existing.setCity(updatedDto.getCity());
        existing.setState(updatedDto.getState());
        existing.setCountry(updatedDto.getCountry());
        existing.setDesignation(updatedDto.getDesignation());

        // Update company by ID only
        if (updatedDto.getCompany() != null && updatedDto.getCompany().getCompanyId() != null) {
            Company company = companyRepository.findById(updatedDto.getCompany().getCompanyId())
                    .orElseThrow(() -> new NoSuchElementException("Company not found: " + updatedDto.getCompany().getCompanyId()));
            existing.setCompany(company);
        }

        return transformToContactDTO(contactRepository.save(existing));
    }


    public ContactDTO transformToContactDTO(Contact contact) {
        if (contact == null) return null;

        ContactDTO dto = new ContactDTO();
        dto.setContactId(contact.getContactId());
        dto.setFirstName(contact.getFirstName());
        dto.setLastName(contact.getLastName());
        dto.setEmailID(contact.getEmailID());
        dto.setLinkedInUrl(contact.getLinkedInUrl());
        dto.setPhoneNo(contact.getPhoneNo());
        dto.setCity(contact.getCity());
        dto.setState(contact.getState());
        dto.setCountry(contact.getCountry());
        dto.setDesignation(contact.getDesignation());

        if (contact.getCompany() != null) {
            CompanyDTO companyDTO = new CompanyDTO();
            companyDTO.setCompanyId(contact.getCompany().getCompanyId());
            companyDTO.setName(contact.getCompany().getName());
            dto.setCompany(companyDTO);
        }

        return dto;
    }

    public Contact transformToContact(ContactDTO dto) {
        if (dto == null) return null;

        Contact contact = new Contact();
        contact.setContactId(dto.getContactId());
        contact.setFirstName(dto.getFirstName());
        contact.setLastName(dto.getLastName());
        contact.setEmailID(dto.getEmailID());
        contact.setLinkedInUrl(dto.getLinkedInUrl());
        contact.setPhoneNo(dto.getPhoneNo());
        contact.setCity(dto.getCity());
        contact.setState(dto.getState());
        contact.setCountry(dto.getCountry());
        contact.setDesignation(dto.getDesignation());

        if (dto.getCompany() != null && dto.getCompany().getCompanyId() != null) {
            Company company = companyRepository.findById(dto.getCompany().getCompanyId())
                    .orElseThrow(() -> new NoSuchElementException("Company not found with id: " + dto.getCompany().getCompanyId()));
            contact.setCompany(company);
        }

        return contact;
    }



}
