package com.accrevent.radius.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;


@Entity
@Data
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long marketingCollectionId;
    private String displayName;

    @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @ToString.Exclude
    private List<MarketingStory> marketingStories = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspaceId", referencedColumnName = "workspaceId")
    @JsonBackReference
    private Workspace workspace;
    private String collectionFolderId;
    private String sharepointUrl;
    private String sharepointPath;

    private String sharepointItemId;


}
