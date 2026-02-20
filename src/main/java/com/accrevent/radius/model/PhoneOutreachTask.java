package com.accrevent.radius.model;

import com.accrevent.radius.util.TaskType;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
@DiscriminatorValue(TaskType.PHONE_OUTREACH_TASK)
@ToString(exclude = {"relatedContact"})
public class PhoneOutreachTask extends Task{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    @JsonManagedReference(value = "contact-phone-outreach")
    private Contact relatedContact;

    private Long didNotAnswer;
    private Long notReachable;

}
