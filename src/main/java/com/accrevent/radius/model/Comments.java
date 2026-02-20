package com.accrevent.radius.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;

import java.time.ZonedDateTime;
@Data
@Entity
public class Comments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    private String commentsTitle;
    @Column(columnDefinition = "TEXT")
    private String commentDescription;
    private String createdBy;
    private Long createdOn;
    @Column(name = "issystemcomment")
    private Boolean isSystemComment = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaignId",referencedColumnName = "campaignId")
    @JsonBackReference(value = "campaign-comments")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunityId",referencedColumnName = "opportunityId")
    @JsonBackReference(value = "opportunity-comments")
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leadId",referencedColumnName = "leadId")
    @JsonBackReference(value = "lead-comments")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taskId",referencedColumnName = "taskId")
    @JsonBackReference(value = "task-comments")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "versionId",referencedColumnName = "versionId")
    @JsonBackReference(value = "version-comments")
    private Version version;
}
