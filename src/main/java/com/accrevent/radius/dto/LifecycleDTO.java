package com.accrevent.radius.dto;

import com.accrevent.radius.model.Campaign;
import com.accrevent.radius.model.Lead;
import com.accrevent.radius.model.Opportunity;
import com.accrevent.radius.model.Task;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data

public class LifecycleDTO {
    private Long lifecycleId;
    private String lifecycleName;
    private Long campaignId;
    private Long opportunityId;
    private Long leadId;
    private Long versionId;
    private String type;
}
