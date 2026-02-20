package com.accrevent.radius.controller;

import com.accrevent.radius.service.CompanyUpdateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/updateContact")
public class MigrateContactController {

    private final CompanyUpdateService companyUpdateService;

    public MigrateContactController(CompanyUpdateService companyUpdateService) {
        this.companyUpdateService = companyUpdateService;
    }
}
