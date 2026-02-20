package com.accrevent.radius.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@DiscriminatorValue("outreach")
@ToString(exclude = {"relatedContact"})
//@JsonIgnoreProperties("relatedContact")
@Data
public class OutreachTask extends Task {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact relatedContact;
}