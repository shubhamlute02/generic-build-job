package com.accrevent.radius.dto;

import com.accrevent.radius.util.TaskType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LinkedInOutreachTaskDTO extends TaskDTO{
    private String contactFirstName;
    private String contactLastName;
    private String contactEmailID;
    private String contactPhoneNo;
    private String contactCompany;
    private Long contactCompanyId;
    private String contactCity;
    private String contactState;
    private String contactCountry;
    private Long contactId;
    private String linkedInUrl;
    private String designation;

    public LinkedInOutreachTaskDTO()
    {
        super();

        setType(TaskType.LINKEDIN_OUTREACH_TASK);
    }
}
