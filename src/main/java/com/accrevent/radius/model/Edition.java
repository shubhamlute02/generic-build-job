package com.accrevent.radius.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Edition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String contentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marketing_story_id")
    private MarketingStory marketingStory;

    @OneToMany(mappedBy = "edition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Version> versions = new ArrayList<>();

    @Column(length = 2000)
    private String sharepointPath;
    @Column(length = 2000)
    private String sharepointUrl;

    @Column(length = 2000)
    private String sharepointItemId;


}
