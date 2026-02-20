package com.accrevent.radius.model;

import com.accrevent.radius.util.TaskType;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
@ToString(exclude = {"relatedContact"})
@DiscriminatorValue(TaskType.LINKEDIN_OUTREACH_TASK)
public class LinkedInOutreachTask extends Task {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    @JsonManagedReference(value = "contact-linkedin-outreach")
    private Contact relatedContact;
}
