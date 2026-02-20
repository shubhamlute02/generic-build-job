package com.accrevent.radius.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class MarketingStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private String purpose;
    private String contentConsumer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marketing_collection_id")
    @JsonBackReference
    @ToString.Exclude
    private Collection collection;

    private String sharepointPath;
    private String sharepointUrl;

    @Column(length = 2000)
    private String sharepointItemId;

    @OneToMany(mappedBy = "marketingStory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Edition> editions = new ArrayList<>();
}
