package com.accrevent.radius.service;

import com.accrevent.radius.model.Company;
import com.accrevent.radius.model.Contact;
import com.accrevent.radius.repository.CompanyRepository;
import com.accrevent.radius.repository.ContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class CompanyUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyUpdateService.class);
    private final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;
    private final PlatformTransactionManager transactionManager;

    public CompanyUpdateService(ContactRepository contactRepository, CompanyRepository companyRepository, PlatformTransactionManager transactionManager){

        this.contactRepository = contactRepository;
        this.companyRepository = companyRepository;
        this.transactionManager = transactionManager;
    }
}
