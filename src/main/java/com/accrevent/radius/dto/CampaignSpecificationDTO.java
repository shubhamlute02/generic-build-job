package com.accrevent.radius.dto;

import com.accrevent.radius.model.Campaign;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;


@Data
public class CampaignSpecificationDTO {
    private Long specificationId;
    private String title;
    private String description;
    private Long campaignId;
}
