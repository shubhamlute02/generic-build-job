package com.accrevent.radius.Serializable;

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Data
public class UserLeadCommentTimeTrack implements Serializable {
    private String userId;
    private Long leadId;

    public UserLeadCommentTimeTrack() {}
    public UserLeadCommentTimeTrack(String userId, Long leadId) {
        this.userId = userId;
        this.leadId = leadId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserLeadCommentTimeTrack that = (UserLeadCommentTimeTrack) o;
        return Objects.equals(userId, that.userId) && Objects.equals(leadId, that.leadId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, leadId);
    }
}
