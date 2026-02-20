package com.accrevent.radius.Serializable;

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Data
public class UserOpportunityCommentTimeTrack implements Serializable {
    private String userId;
    private Long opportunityId;

    public UserOpportunityCommentTimeTrack() {}
    public UserOpportunityCommentTimeTrack(String userId, Long opportunityId) {
        this.userId = userId;
        this.opportunityId = opportunityId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserOpportunityCommentTimeTrack that = (UserOpportunityCommentTimeTrack) o;
        return Objects.equals(userId, that.userId) && Objects.equals(opportunityId, that.opportunityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, opportunityId);
    }
}
