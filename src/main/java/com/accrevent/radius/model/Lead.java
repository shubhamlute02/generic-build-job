package com.accrevent.radius.model;

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
public class Lead {

    @Id
    @ToString.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long leadId;

    @ToString.Include
    private String leadName;

    @ToString.Include
    private String leadTitle;

    @ToString.Include
    private String description;
    private Long createdOn;
    @Column(name = "issystemcomment")
    private  Boolean isSystemComment=false;
    private String businessUnit;

    @Column(name = "isarchived", columnDefinition = "boolean default false")
    private Boolean isArchived = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaignId", nullable = false,referencedColumnName ="campaignId")
    @JsonBackReference
    private Campaign campaign;

    @OneToMany(mappedBy = "lead",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    @JsonManagedReference(value = "lead-lifecycle")
    private List<Lifecycle> leadlifecycleList = new ArrayList<>();

    @OneToMany(mappedBy = "lead",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    @JsonManagedReference(value = "lead-comments")
    private List<Comments> commentsList = new ArrayList<>();

    @OneToMany(mappedBy = "lead",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<UserLeadSequence> userLeadSequenceList = new ArrayList<>();

    // Add a safe virtual field to show campaignId in toString
    @ToString.Include(name = "campaignId")
    public Long getCampaignIdForToString() {
    return campaign != null ? campaign.getCampaignId() : null;
}


}
