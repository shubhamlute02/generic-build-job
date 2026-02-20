package com.accrevent.radius.model;

import com.accrevent.radius.util.CampaignType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@ToString(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Campaign {
    @Id
    @ToString.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long campaignId;

    @Column(nullable = false)
    @ToString.Include
    private String campaignName;

    @ToString.Include
    private String description;

    @ToString.Include
    private Long createdOn;

    @ToString.Include
    private String Owner;

    @Column(name = "issystemcomment")
    @ToString.Include
    private Boolean isSystemComment=false;

    @Column(name = "isarchived", columnDefinition = "boolean default false")
    private Boolean isArchived = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspaceId", nullable = false,referencedColumnName ="workspaceId")
    @JsonBackReference
    private Workspace workspace;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Lead> leadList = new ArrayList<>();

    @OneToMany(mappedBy = "campaign",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    @JsonManagedReference(value = "campaign-lifecycle")
    private List<Lifecycle> lifecycleList = new ArrayList<>();


    @OneToMany(mappedBy = "campaign",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    @JsonManagedReference(value = "campaign-comments")
    private List<Comments> commentsList = new ArrayList<>();

    @OneToMany(mappedBy = "campaign",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<UserCampaignSequence> userCampaignSequenceList = new ArrayList<>();

    @OneToMany(mappedBy = "campaign",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CampaignSpecification> campaignSpecificationList = new ArrayList<>();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<MarketingMeetingRegister> marketingMeetingRegisterList = new ArrayList<>();


    @Column(nullable = false, length = 20)
    private String type = CampaignType.DEFAULT;


    private Long plannedStartDate;
    private Long actualStartDate;
    private Long plannedEndDate;
    private Long actualEndDate;

}
