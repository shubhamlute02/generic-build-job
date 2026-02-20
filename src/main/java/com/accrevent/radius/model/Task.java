package com.accrevent.radius.model;

import com.accrevent.radius.util.TaskType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
//added2 new annotations
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "task_type")
@DiscriminatorValue(TaskType.DEFAULT)
@ToString(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long taskId;

    @ToString.Include
    private String taskName;
    private String description;

    @ToString.Include
    private String assignTo;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean sendToAssignee;
    private String duration;
    private Integer durationValue;
    private Long dueDate;
    @Column(name = "created_on")
    private Long createdOn;
    @Column(name = "issystemcomment")
    private  Boolean isSystemComment=false;

    @Column(nullable = false)
    @ToString.Include
    private String status;

    @Column(columnDefinition = "INT DEFAULT 99")
    private int weeklyTaskSequence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lifecycleId",referencedColumnName = "lifecycleId", nullable = true)
    @JsonBackReference(value = "lifecycle-task")
    private Lifecycle lifecycle;

    @Column(name = "task_type", insertable = false, updatable = false)
    private String type;


    @ManyToOne
    @JoinColumn(name = "parentTaskId",referencedColumnName = "taskId")
    @JsonBackReference(value = "task-parent")
    private Task parentTask;

    @OneToMany(mappedBy = "parentTask", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference(value = "task-parent")
    private List<Task> subTasks;

    @OneToMany(mappedBy = "task",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    @JsonManagedReference(value = "task-comments")
    private List<Comments> commentsList = new ArrayList<>();

    @OneToMany(mappedBy = "task",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    @JsonManagedReference(value = "task-sequence")
    private List<UserTaskSequence> userTaskSequenceList = new ArrayList<>();

    // Virtual field for logging lifecycleId without lazy loading
    @ToString.Include(name = "lifecycleId")
    public Long getLifecycleIdForToString() {
        return lifecycle != null ? lifecycle.getLifecycleId() : null;
    }
}
