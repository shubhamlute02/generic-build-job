package com.accrevent.radius.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Version {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long versionId;
    private String version;
    private String description;

    @Column(length = 2000)
    private String sharepointPath;

    @Column(length = 2000)
    private String sharepointUrl;


    @Column(length = 2000)
    private String sharepointItemId;

    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edition_id")
    private Edition edition;


    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "version-lifecycle")
    private List<Lifecycle> lifecycles = new ArrayList<>();

    private Long approvedAt;


    @OneToMany(mappedBy = "version",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    @JsonManagedReference(value = "version-comments")
    private List<Comments> commentsList = new ArrayList<>();
}
