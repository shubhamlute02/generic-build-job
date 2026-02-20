package com.accrevent.radius.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class UserRegion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userRegionId;
    @Column(nullable = false,unique = true)
    private String userId;
    private String region;
}
