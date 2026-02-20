package com.accrevent.radius.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Opportunity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long opportunityId;

    @Column(nullable = false)
    private String opportunityName;
    private String description;
    private String requirement;
    private String customer;
    private String estimateRevenue;
    private String currency;
    private String projectTitle;
    private String businessUnit;
    private Long createdOn;

    @Transient   // ✅ not saved to DB, but appears in JSON
    private String path;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspaceId", nullable = false,referencedColumnName ="workspaceId")
    @JsonBackReference
    private Workspace workspace;

    @Column(name = "issystemcomment")
    private  Boolean isSystemComment=false;

    @Column(name = "isarchived", columnDefinition = "boolean default false")
    private Boolean isArchived = false;


    @OneToMany(mappedBy = "opportunity",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    @JsonManagedReference(value = "opportunity-lifecycle")
    private List<Lifecycle> opportunitylifecycleList = new ArrayList<>();

    @OneToMany(mappedBy = "opportunity",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    @JsonManagedReference(value = "opportunity-comments")
    private List<Comments> commentsList = new ArrayList<>();

    @OneToMany(mappedBy = "opportunity",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<UserOpportunitySequence> userOpportunitySequenceList = new ArrayList<>();
}
