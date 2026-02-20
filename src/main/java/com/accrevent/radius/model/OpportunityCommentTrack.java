package com.accrevent.radius.model;

import com.accrevent.radius.Serializable.UserOpportunityCommentTimeTrack;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.Data;

import java.time.ZonedDateTime;

@Entity
@IdClass(UserOpportunityCommentTimeTrack.class)
@Data
public class OpportunityCommentTrack {
    @Id
    private String userId;
    @Id
    private Long opportunityId;
    private ZonedDateTime dateTime;
}
