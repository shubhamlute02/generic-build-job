package com.accrevent.radius.dto;

import jakarta.persistence.*;
import lombok.Data;

@Data
public class UserRegionDTO {
    private Long userRegionId;
    private String userId;
    private String region;
}
