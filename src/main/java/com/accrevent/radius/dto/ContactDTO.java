package com.accrevent.radius.dto;

import lombok.Data;

@Data
public class ContactDTO {
    private Long contactId;
    private String firstName;
    private String lastName;
    private String emailID;
    private String linkedInUrl;
    private String phoneNo;
    private String city;
    private String state;
    private String country;
    private CompanyDTO company;
    private String designation;
}
