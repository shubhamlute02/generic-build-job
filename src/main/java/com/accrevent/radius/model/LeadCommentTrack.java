package com.accrevent.radius.model;

import com.accrevent.radius.Serializable.UserLeadCommentTimeTrack;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.Data;

import java.time.ZonedDateTime;

@Entity
@IdClass(UserLeadCommentTimeTrack.class)
@Data
public class LeadCommentTrack {
    @Id
    private String userId;
    @Id
    private Long leadId;
    private ZonedDateTime dateTime;
}
