package com.accrevent.radius.Serializable;

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Data
public class UserCampaignCommentTimeTrack implements Serializable {
    private String userId;
    private Long campaignId;

    public UserCampaignCommentTimeTrack() {}
    public UserCampaignCommentTimeTrack(String userId, Long campaignId) {
        this.userId = userId;
        this.campaignId = campaignId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserCampaignCommentTimeTrack that = (UserCampaignCommentTimeTrack) o;
        return Objects.equals(userId, that.userId) && Objects.equals(campaignId, that.campaignId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, campaignId);
    }
}
