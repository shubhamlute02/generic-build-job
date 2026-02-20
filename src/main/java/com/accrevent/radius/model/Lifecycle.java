package com.accrevent.radius.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Lifecycle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lifecycleId;
    private String lifecycleName;
    private String status;

    //for which tasklifecycle should assign
    @Column(nullable = false, columnDefinition = "varchar(255) default 'default'")
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaignId",referencedColumnName = "campaignId")
    @JsonBackReference(value = "campaign-lifecycle")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunityId",referencedColumnName = "opportunityId")
    @JsonBackReference(value = "opportunity-lifecycle")
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leadId",referencedColumnName = "leadId")
    @JsonBackReference(value = "lead-lifecycle")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "versionId",referencedColumnName = "versionId")
    @JsonBackReference(value = "version-lifecycle")
    private Version version;

    @OneToMany(mappedBy = "lifecycle",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    @JsonManagedReference(value = "lifecycle-task")
    private List<Task> taskList = new ArrayList<>();
}
