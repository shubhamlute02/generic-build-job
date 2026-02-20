package com.accrevent.radius.model;

import com.accrevent.radius.Serializable.UserCampaignCommentTimeTrack;
import com.accrevent.radius.Serializable.UserTaskCommentTimeTrack;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.Data;

import java.time.ZonedDateTime;

@Entity
@IdClass(UserCampaignCommentTimeTrack.class)
@Data
public class CampaignCommentTrack {
    @Id
    private String userId;
    @Id
    private Long campaignId;
    private ZonedDateTime dateTime;
}
