package com.accrevent.radius.model;

import com.accrevent.radius.util.TaskType;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@DiscriminatorValue(TaskType.EMAIL_OUTREACH_TASK)
@ToString(exclude = {"relatedContact"})
@Data
public class EmailOutreachTask extends Task {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    @JsonManagedReference(value = "contact-email-outreach")
    private Contact relatedContact;
}