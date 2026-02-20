package com.accrevent.radius.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@ToString(exclude = {"tasks"})
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long contactId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Company company;

    private String firstName;
    private String lastName;
    private String emailID;
    private String linkedInUrl;
    private String phoneNo;
    private String city;
    private String state;
    private String country;
    private String designation;

    @OneToMany(mappedBy = "relatedContact", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference(value = "contact-email-outreach")
    private List<EmailOutreachTask> relatedEmailOutreachTasks = new ArrayList<>();


    @OneToMany(mappedBy = "relatedContact", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference(value = "contact-phone-outreach")
    private List<PhoneOutreachTask> relatedPhoneOutreachTasks = new ArrayList<>();

    @OneToMany(mappedBy = "relatedContact", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference(value = "contact-linkedin-outreach")
    private List<LinkedInOutreachTask> relatedLinkedInOutreachTasks = new ArrayList<>();

}
