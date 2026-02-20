package com.accrevent.radius.service;

import com.accrevent.radius.dto.*;
import com.accrevent.radius.model.*;
import com.accrevent.radius.model.Collection;
import com.accrevent.radius.repository.*;
//import com.accrevent.radius.scheduling.TaskMoveScheduler;
import com.accrevent.radius.util.*;
import jakarta.persistence.DiscriminatorValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {
    Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private  final ConstantLifecycleRepository constantLifecycleRepository;
    private final LifecycleRepository lifecycleRepository;
    private final UserTaskSequenceRepository userTaskSequenceRepository;
    private final CommentsRepository commentsRepository;
    private final TaskCustomRepository taskCustomRepository;
    private final EntityManager entityManager;
    private final EmailOutreachCampaignMaturityRecordRegisterService maturityService;
    private final LinkedInOutreachCampaignMaturityRecordRegisterService linkedInOutreachCampaignMaturityRecordRegisterService;
    private final PhoneOutreachCampaignMaturityRecordRegisterService phoneOutreachCampaignMaturityRecordRegisterService;
    private final UserRegionService userRegionService;
    private final ContactRepository contactRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CompanyRepository companyRepository;
    private final LeadService leadService;
    private final PhoneCallingRegisterRepository phoneCallingRegisterRepository;
    private final PhoneCallingRegisterService phoneCallingRegisterService;

    private final MarketingMeetingRegisterService marketingMeetingRegisterService;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E dd-MMM-yyyy");

    public TaskService(TaskRepository taskRepository, ConstantLifecycleRepository constantLifecycleRepository,
                       LifecycleRepository lifecycleRepository,
                       UserTaskSequenceRepository userTaskSequenceRepository,
                       CommentsRepository commentsRepository, TaskCustomRepository taskCustomRepository, EntityManager entityManager, EmailOutreachCampaignMaturityRecordRegisterService maturityService, LinkedInOutreachCampaignMaturityRecordRegisterService linkedInOutreachCampaignMaturityRecordRegisterService, PhoneOutreachCampaignMaturityRecordRegisterService phoneOutreachCampaignMaturityRecordRegisterService, UserRegionService userRegionService, ContactRepository contactRepository, WorkspaceRepository workspaceRepository, TeamMemberRepository teamMemberRepository, CompanyRepository companyRepository, LeadService leadService, PhoneCallingRegisterRepository phoneCallingRegisterRepository,PhoneCallingRegisterService phoneCallingRegisterService, MarketingMeetingRegisterService marketingMeetingRegisterService)
    {
        this.taskRepository = taskRepository;
        this.constantLifecycleRepository = constantLifecycleRepository;
        this.lifecycleRepository = lifecycleRepository;
        this.userTaskSequenceRepository = userTaskSequenceRepository;
        this.commentsRepository = commentsRepository;
        this.taskCustomRepository = taskCustomRepository;
        this.entityManager = entityManager;
        this.maturityService = maturityService;
        this.linkedInOutreachCampaignMaturityRecordRegisterService = linkedInOutreachCampaignMaturityRecordRegisterService;
        this.phoneOutreachCampaignMaturityRecordRegisterService = phoneOutreachCampaignMaturityRecordRegisterService;

        this.userRegionService = userRegionService;
        this.contactRepository = contactRepository;
        this.workspaceRepository = workspaceRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.companyRepository = companyRepository;
        this.leadService = leadService;
        this.phoneCallingRegisterRepository = phoneCallingRegisterRepository;
        this.phoneCallingRegisterService = phoneCallingRegisterService;
        this.marketingMeetingRegisterService = marketingMeetingRegisterService;
    }

    private List<TaskWeeklyPlannerDTO> getTasksDueTodayForUser(String user,   Set<Long> ownedWorkspaceIds) {

        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        long startTime = 0L;

        // End time = end of today
        long endTime = today.plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli() - 1;

        // Get tasks for this user due today
        List<TaskWeeklyPlannerDTO> tasks = taskRepository.getTasksByAssignToAndSendToAssigneeAndDueDateRange(
                user,
                true, // sendToAssignee
                startTime,
                endTime
        );

        tasks.removeIf(taskDto -> {
            Optional<Task> taskOpt = taskRepository.findById(taskDto.getTaskId());

            if (taskOpt.isEmpty()) {
                return true; // remove if task not found
            }

            Task task = taskOpt.get();

            // WORKSPACE OWNERSHIP CHECK
            Lifecycle lifecycle = task.getLifecycle();

            //for including subtask
            if (lifecycle == null && task.getParentTask() != null) {
                lifecycle = task.getParentTask().getLifecycle();
            }

            if (lifecycle == null) {
                return true;
            }

            Workspace workspace = null;

            if (lifecycle.getLead() != null) {
                workspace = lifecycle.getLead()
                        .getCampaign()
                        .getWorkspace();
            } else if (lifecycle.getCampaign() != null) {
                workspace = lifecycle.getCampaign()
                        .getWorkspace();
            }else if (lifecycle.getOpportunity() != null) {
                workspace = lifecycle.getOpportunity()
                        .getWorkspace();

            }

            if (workspace == null) {
                return true;
            }

            return !ownedWorkspaceIds.contains(workspace.getWorkspaceId());
        });


        // Enrich tasks with type information
        if (tasks != null) {
            tasks.removeIf(task -> {
                // find lifecycle stages for the task type
                Long cycleId = task.getType() == TaskType.EMAIL_OUTREACH_TASK ? 5L : 6L;
                List<ConstantLifecycle> stages = constantLifecycleRepository.findByCycleId(cycleId);

                if (stages.isEmpty()) {
                    logger.warn("No lifecycle stages defined for type: {}", task.getType());
                    return false; // don’t exclude anything if lifecycle missing
                }

                // dynamically get last stage
                String finalStageStatus = stages.get(stages.size() - 1).getCycleName();
                logger.debug("Final stage for {} = {}", task.getType(), finalStageStatus);

                // exclude task if it’s at final stage
                return finalStageStatus.equalsIgnoreCase(task.getStatus());
            });

            tasks.forEach(task -> {
                // Get the full task entity to determine type
                Optional<Task> fullTaskOpt = taskRepository.findById(task.getTaskId());
                if (fullTaskOpt.isPresent()) {
                    Task fullTask = fullTaskOpt.get();

                    // Set type based on actual task class using string constants
                    if (fullTask instanceof EmailOutreachTask) {
                        task.setType(TaskType.EMAIL_OUTREACH_TASK);
                        EmailOutreachTask emailOutreachTask = (EmailOutreachTask) fullTask;

                        // Add contact details if available
                        if (emailOutreachTask.getRelatedContact() != null) {
                            Contact contact = emailOutreachTask.getRelatedContact();
                            task.setContactId(contact.getContactId());
                            task.setContactFirstName(contact.getFirstName());
                            task.setContactLastName(contact.getLastName());
                            task.setContactEmailID(contact.getEmailID());
                            task.setContactPhoneNo(contact.getPhoneNo());
//                            task.setContactCompany(contact.getCompany());
                            task.setContactCompany(contact.getCompany().getName());
                            task.setContactCity(contact.getCity());
                            task.setContactState(contact.getState());
                            task.setContactCountry(contact.getCountry());
                            task.setLinkedInUrl(contact.getLinkedInUrl());
                        }
                    } else {
                        task.setType(TaskType.DEFAULT);
                    }
                }
            });
        }

        return tasks;
    }

    public AssignedToOthersTasksDTO getAssignedToOthersTasks(String currentUser) {
        AssignedToOthersTasksDTO response = new AssignedToOthersTasksDTO();
        List<AssignedToOthersTasksDTO.UserTasks> userTasksList = new ArrayList<>();

        // Find all workspaces where the current user is the owner
        List<Workspace> ownedWorkspaces = workspaceRepository.findByOwner(currentUser);
        Set<Long> ownedWorkspaceIds = ownedWorkspaces.stream()
                .map(Workspace::getWorkspaceId)
                .collect(Collectors.toSet());

        logger.info("User '{}' owns {} workspaces: {}", currentUser, ownedWorkspaceIds.size(), ownedWorkspaceIds);

        //  Get all team members in those workspaces (excluding current user)
        Set<String> otherUsers = new HashSet<>();
        for (Workspace workspace : ownedWorkspaces) {
            List<TeamMember> teamMembers = teamMemberRepository.findByWorkspace_WorkspaceId(workspace.getWorkspaceId());
            teamMembers.stream()
                    .map(TeamMember::getUserId)
                    .filter(userId -> !userId.equals(currentUser))
                    .forEach(otherUsers::add);
        }
        logger.info("Other team members found: {}", otherUsers);

        // For each other user, fetch tasks due today within the owned workspaces
        for (String user : otherUsers) {
            logger.info("Fetching tasks for user '{}'", user);

            List<TaskWeeklyPlannerDTO> tasksDueToday = getTasksDueTodayForUser(user, ownedWorkspaceIds);
            logger.info("User '{}' has {} tasks after workspace filtering", user, tasksDueToday.size());

            if (!tasksDueToday.isEmpty()) {
                // Override path for each task
                tasksDueToday.forEach(dto -> taskRepository.findById(dto.getTaskId()).ifPresent(fullTask -> {
                    dto.setPath(buildTaskPath(fullTask));
                }));

                AssignedToOthersTasksDTO.UserTasks userTasks = new AssignedToOthersTasksDTO.UserTasks();
                userTasks.setUser(user);
                userTasks.setTasks(tasksDueToday);
                userTasksList.add(userTasks);
            }
        }

        response.setAssignedToOthersTasks(userTasksList);
        return response;
    }


    public TaskDTO createTask(TaskDTO taskDTO) {
        // Validate not outreach
        if(TaskType.EMAIL_OUTREACH_TASK.equalsIgnoreCase(taskDTO.getType())) {
            throw new IllegalArgumentException("Use createOutreachTask for outreach tasks");
        }

        Task task = transformToTask(taskDTO);
        task = taskRepository.save(task);

        // Generate system comment: "<FullName> created the task."
        String username = RadiusUtil.getCurrentUsername();


        StringBuilder commentBuilder = new StringBuilder();
        commentBuilder.append(username)
                .append(" created the task.");

        Comments comment = new Comments();
        comment.setTask(task);
        comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
        comment.setCommentDescription(commentBuilder.toString());
        comment.setCreatedOn(System.currentTimeMillis());
        comment.setIsSystemComment(true);

        commentsRepository.save(comment);

        System.out.println("CreatedOn after save: " + task.getCreatedOn());

        return transformToTaskDTO(task);
    }


    public EmailOutreachTaskDTO createEmailOutreachTask(EmailOutreachTaskDTO emailOutreachTaskDTO) {
        EmailOutreachTask task = (EmailOutreachTask) transformToTask(emailOutreachTaskDTO);
        task = taskRepository.save(task);

        // Generate system comment: "<FullName> created the task."
        String username = RadiusUtil.getCurrentUsername();

        StringBuilder commentBuilder = new StringBuilder();
        commentBuilder.append(username)
                .append(" created the task.");

        Comments comment = new Comments();
        comment.setTask(task);
        comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
        comment.setCommentDescription(commentBuilder.toString());
        comment.setCreatedOn(System.currentTimeMillis());
        comment.setIsSystemComment(true);

        commentsRepository.save(comment);

        System.out.println("CreatedOn after save: " + task.getCreatedOn());
        return transformToEmailOutreachTaskDTO(task);
    }

    @Transactional
    public PhoneOutreachTaskDTO updatePhoneOutreachTask(PhoneOutreachTaskDTO phoneOutreachTaskDTO) {

        // 1. Get and validate existing task
        Optional<Task> opt = taskRepository.findById(phoneOutreachTaskDTO.getTaskId());
        if (!opt.isPresent()) {
            throw new IllegalArgumentException("Task does not exist");
        }

        PhoneOutreachTask phoneOutreachTask = (PhoneOutreachTask) opt.get();

        StringBuilder changes = new StringBuilder();
        String username = RadiusUtil.getCurrentUsername();
        boolean nonStatusChangesMade = false;

        // Task Name
        if (phoneOutreachTask.getTaskName() == null) {
            if (phoneOutreachTaskDTO.getTaskName() != null) {
                changes.append(username)
                        .append(" updated the Title. Old Value: '")
                        .append(phoneOutreachTask.getTaskName())
                        .append("'  New Value: '")
                        .append(phoneOutreachTaskDTO.getTaskName()).append("'");
                nonStatusChangesMade = true;
            }
        } else if (phoneOutreachTaskDTO.getTaskName() == null) {
            changes.append(username)
                    .append(" updated the Title. Old Value: '")
                    .append(phoneOutreachTask.getTaskName())
                    .append("'  New Value: '")
                    .append(phoneOutreachTaskDTO.getTaskName()).append("'");
            nonStatusChangesMade = true;
        } else if (!Objects.equals(phoneOutreachTask.getTaskName(), phoneOutreachTaskDTO.getTaskName())) {
            changes.append(username)
                    .append(" updated the Title. Old Value: '")
                    .append(phoneOutreachTask.getTaskName())
                    .append("'  New Value: '")
                    .append(phoneOutreachTaskDTO.getTaskName()).append("'");
            nonStatusChangesMade = true;
        }

        // Description
        if ((phoneOutreachTask.getDescription() == null || phoneOutreachTask.getDescription().isEmpty() || phoneOutreachTask.getDescription().equalsIgnoreCase("No Description"))
                && phoneOutreachTaskDTO.getDescription() != null && !phoneOutreachTaskDTO.getDescription().isEmpty()
                && !phoneOutreachTaskDTO.getDescription().equalsIgnoreCase("No Description")) {
            changes.append(username)
                    .append(" added a Description: ")
                    .append(phoneOutreachTaskDTO.getDescription())
                    .append(". ");
            nonStatusChangesMade = true;

        }
        else if(phoneOutreachTaskDTO.getDescription()== null && phoneOutreachTask.getDescription() != null){
            changes.append(username)
                    .append(" updated the Description. Old Value: '")
                    .append(phoneOutreachTask.getDescription())
                    .append("'  New Value: '")
                    .append(phoneOutreachTaskDTO.getDescription()).append("'");
            nonStatusChangesMade = true;
        }
        else if (!Objects.equals(phoneOutreachTask.getDescription(), phoneOutreachTaskDTO.getDescription())) {
            changes.append(username)
                    .append(" updated the Description. Old Value: '")
                    .append(phoneOutreachTask.getDescription())
                    .append("'  New Value: '")
                    .append(phoneOutreachTaskDTO.getDescription()).append("'");
            nonStatusChangesMade = true;
        }

        // Compare AssignTo change
        if ((phoneOutreachTask.getAssignTo() == null || phoneOutreachTask.getAssignTo().isEmpty())
                && phoneOutreachTaskDTO.getAssignTo() != null && !phoneOutreachTaskDTO.getAssignTo().isEmpty()) {
            changes.append(username)
                    .append(" assigned the task to: ")
                    .append(phoneOutreachTaskDTO.getAssignTo())
                    .append(". ");
            nonStatusChangesMade = true;

        }
        else if(phoneOutreachTaskDTO.getAssignTo()== null){
            changes.append(username)
                    .append(" changed the Assignee from '")
                    .append(phoneOutreachTask.getAssignTo())
                    .append("' to '")
                    .append(phoneOutreachTaskDTO.getAssignTo()).append("'");
            nonStatusChangesMade = true;
        }
        else if (!Objects.equals(phoneOutreachTask.getAssignTo(), phoneOutreachTaskDTO.getAssignTo())) {
            changes.append(username)
                    .append(" changed the Assignee from '")
                    .append(phoneOutreachTask.getAssignTo())
                    .append("' to '")
                    .append(phoneOutreachTaskDTO.getAssignTo()).append("'");
            nonStatusChangesMade = true;

        }

        //Compare next work duration change(working with additional comment)
        if(phoneOutreachTask.getDuration() == null)
        {
            if(phoneOutreachTaskDTO.getDuration()!= null){
                changes.append(username)
                        .append(" changed the Next Work Duration from '")
                        .append(phoneOutreachTask.getDuration())
                        .append("' to '")
                        .append(phoneOutreachTaskDTO.getDuration()).append("'");
                nonStatusChangesMade = true;
            }
        }else if(phoneOutreachTaskDTO.getDuration() == null && phoneOutreachTask.getDuration() != null){
            changes.append(username)
                    .append(" changed the Next Work Duration from '")
                    .append(phoneOutreachTask.getDuration())
                    .append("' to '")
                    .append(phoneOutreachTaskDTO.getDuration()).append("'");
            nonStatusChangesMade = true;
        }
        else if(!Objects.equals(phoneOutreachTask.getDuration(), phoneOutreachTaskDTO.getDuration()))
        {
            changes.append(username)
                    .append("'  changed the Next Work Duration from '")
                    .append(phoneOutreachTask.getDuration())
                    .append("' to '")
                    .append(phoneOutreachTaskDTO.getDuration()).append("'");
            nonStatusChangesMade = true;
        }

        //compare next work date changes
        if(phoneOutreachTask.getDueDate()!= null) {
            if (!phoneOutreachTask.getDueDate().equals((phoneOutreachTaskDTO.getDueDate()))) {
                String oldDateFormatted = Instant.ofEpochMilli(phoneOutreachTask.getDueDate())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(formatter);

                String newDateFormatted = Instant.ofEpochMilli(phoneOutreachTaskDTO.getDueDate())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(formatter);
                changes.append(username)
                        .append(" has changed the Next Work Date from '")
                        .append(oldDateFormatted)
                        .append("' to '")
                        .append(newDateFormatted)
                        .append("'");
                nonStatusChangesMade = true;
            }
        }

        if(phoneOutreachTask.getParentTask() == null)
        {
            if(phoneOutreachTaskDTO.getParentTaskId()!= null && phoneOutreachTaskDTO.getParentTaskId() != 0){
                changes.append(username)
                        .append(" has updated the parent task from '")
                        .append(Objects.toString(phoneOutreachTask.getParentTask(), "None"))
                        .append("' to '")
                        .append(phoneOutreachTaskDTO.getParentTaskId()).append("'");
                nonStatusChangesMade = true;
            }
        }else if(phoneOutreachTaskDTO.getParentTaskId()== null)
        {
            changes.append(username)
                    .append(" has updated the parent task from '")
                    .append(phoneOutreachTask.getParentTask().getTaskId())
                    .append("' to '")
                    .append(phoneOutreachTaskDTO.getParentTaskId()).append("'");
            nonStatusChangesMade = true;
        }
        else if(phoneOutreachTask.getParentTask()!= null) {
            if (!phoneOutreachTask.getParentTask().getTaskId().equals(phoneOutreachTaskDTO.getParentTaskId())) {
                changes.append(username)
                        .append(" has updated the parent task from '")
                        .append(phoneOutreachTask.getParentTask().getTaskId())
                        .append("' to '")
                        .append(phoneOutreachTaskDTO.getParentTaskId()).append("'");
                nonStatusChangesMade = true;
            }
        }

        // 4. Handle task movement using the new path builders
//        String oldPath = buildFullPhoneOutreachTaskPathForComment(phoneOutreachTask);
//        String newPath = buildFullPhoneOutreachTaskPathForComment(phoneOutreachTaskDTO);
//        if (!oldPath.equals(newPath)) {
//            changes.append(username)
//                    .append(" moved task from: ")
//                    .append(oldPath)
//                    .append(" to: ")
//                    .append(newPath);
//            nonStatusChangesMade = true;
//        }

        // 5. Handle status changes
        String existingStatus = phoneOutreachTask.getStatus();
        String newStatus = phoneOutreachTaskDTO.getStatus();
        boolean statusChanged = !Objects.equals(existingStatus, newStatus);

        if (statusChanged) {
            changes.append(username)
                    .append(" updated Status from '")
                    .append(existingStatus != null ? existingStatus : "None")
                    .append("' to '")
                    .append(newStatus != null ? newStatus : "None")
                    .append("'");
        }

        // 6. Transform and save
        PhoneOutreachTask updatedTask = transformToPhoneOutreachTask(phoneOutreachTaskDTO);
        Contact updatedContact = transformToContact(phoneOutreachTaskDTO);
        updatedContact = contactRepository.save(updatedContact);
        updatedTask.setRelatedContact(updatedContact);
        Task savedTask = taskRepository.save(updatedTask);

        // 7. Save system comment if changes were made
        boolean isSystemComment = false;
        if (changes.length() > 0) {
            Comments comment = new Comments();
            comment.setTask(savedTask);
            comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
            comment.setCommentDescription(changes.toString());
            comment.setCreatedOn(System.currentTimeMillis());
            comment.setIsSystemComment(true);
            commentsRepository.save(comment);
            isSystemComment = true;
        }

        // 8. Return updated DTO
        PhoneOutreachTaskDTO updatedDTO = transformToPhoneOutreachTaskDTO((PhoneOutreachTask) savedTask);
        updatedDTO.setIsSystemComment(isSystemComment);
        return updatedDTO;

    }


    @Transactional
    public EmailOutreachTaskDTO updateEmailOutreachTask(EmailOutreachTaskDTO emailOutreachTaskDTO) {

        // 1. Get and validate existing task
        Optional<Task> opt = taskRepository.findById(emailOutreachTaskDTO.getTaskId());
        if (!opt.isPresent()) {
            throw new IllegalArgumentException("Task does not exist");
        }

        EmailOutreachTask emailOutreachTask = (EmailOutreachTask) opt.get();

        StringBuilder changes = new StringBuilder();
        String username = RadiusUtil.getCurrentUsername();
        boolean nonStatusChangesMade = false;


        // Task Name
        if (emailOutreachTask.getTaskName() == null) {
            if (emailOutreachTaskDTO.getTaskName() != null) {
                changes.append(username)
                        .append(" updated the Title. Old Value: '")
                        .append(emailOutreachTask.getTaskName())
                        .append("'  New Value: '")
                        .append(emailOutreachTaskDTO.getTaskName()).append("'");
                nonStatusChangesMade = true;
            }
        } else if (emailOutreachTaskDTO.getTaskName() == null) {
            changes.append(username)
                    .append(" updated the Title. Old Value: '")
                    .append(emailOutreachTask.getTaskName())
                    .append("'  New Value: '")
                    .append(emailOutreachTaskDTO.getTaskName()).append("'");
            nonStatusChangesMade = true;
        } else if (!Objects.equals(emailOutreachTask.getTaskName(), emailOutreachTaskDTO.getTaskName())) {
            changes.append(username)
                    .append(" updated the Title. Old Value: '")
                    .append(emailOutreachTask.getTaskName())
                    .append("'  New Value: '")
                    .append(emailOutreachTaskDTO.getTaskName()).append("'");
            nonStatusChangesMade = true;
        }

        // Description
        if ((emailOutreachTask.getDescription() == null || emailOutreachTask.getDescription().isEmpty() || emailOutreachTask.getDescription().equalsIgnoreCase("No Description"))
                && emailOutreachTaskDTO.getDescription() != null && !emailOutreachTaskDTO.getDescription().isEmpty()
                && !emailOutreachTaskDTO.getDescription().equalsIgnoreCase("No Description")) {
            changes.append(username)
                    .append(" added a Description: ")
                    .append(emailOutreachTaskDTO.getDescription())
                    .append(". ");
            nonStatusChangesMade = true;

        }
        else if(emailOutreachTaskDTO.getDescription()== null && emailOutreachTask.getDescription() != null){
            changes.append(username)
                    .append(" updated the Description. Old Value: '")
                    .append(emailOutreachTask.getDescription())
                    .append("'  New Value: '")
                    .append(emailOutreachTaskDTO.getDescription()).append("'");
            nonStatusChangesMade = true;
        }
        else if (!Objects.equals(emailOutreachTask.getDescription(), emailOutreachTaskDTO.getDescription())) {
            changes.append(username)
                    .append(" updated the Description. Old Value: '")
                    .append(emailOutreachTask.getDescription())
                    .append("'  New Value: '")
                    .append(emailOutreachTaskDTO.getDescription()).append("'");
            nonStatusChangesMade = true;
        }

        // Compare AssignTo change
        if ((emailOutreachTask.getAssignTo() == null || emailOutreachTask.getAssignTo().isEmpty())
                && emailOutreachTaskDTO.getAssignTo() != null && !emailOutreachTaskDTO.getAssignTo().isEmpty()) {
            changes.append(username)
                    .append(" assigned the task to: ")
                    .append(emailOutreachTaskDTO.getAssignTo())
                    .append(". ");
            nonStatusChangesMade = true;

        }
        else if(emailOutreachTaskDTO.getAssignTo()== null){
            changes.append(username)
                    .append(" changed the Assignee from '")
                    .append(emailOutreachTask.getAssignTo())
                    .append("' to '")
                    .append(emailOutreachTaskDTO.getAssignTo()).append("'");
            nonStatusChangesMade = true;
        }
        else if (!Objects.equals(emailOutreachTask.getAssignTo(), emailOutreachTaskDTO.getAssignTo())) {
            changes.append(username)
                    .append(" changed the Assignee from '")
                    .append(emailOutreachTask.getAssignTo())
                    .append("' to '")
                    .append(emailOutreachTaskDTO.getAssignTo()).append("'");
            nonStatusChangesMade = true;

        }

        //Compare duration change(working with additional comment)
        if(emailOutreachTask.getDuration() == null)
        {
            if(emailOutreachTaskDTO.getDuration()!= null){
                changes.append(username)
                        .append(" changed the Next Work Duration from '")
                        .append(emailOutreachTask.getDuration())
                        .append("' to '")
                        .append(emailOutreachTaskDTO.getDuration()).append("'");
                nonStatusChangesMade = true;
            }
        }else if(emailOutreachTaskDTO.getDuration() == null && emailOutreachTask.getDuration() != null){
            changes.append(username)
                    .append(" changed the Next Work Duration from '")
                    .append(emailOutreachTask.getDuration())
                    .append("' to '")
                    .append(emailOutreachTaskDTO.getDuration()).append("'");
            nonStatusChangesMade = true;
        }
        else if(!Objects.equals(emailOutreachTask.getDuration(), emailOutreachTaskDTO.getDuration()))
        {
            changes.append(username)
                    .append("'  changed the Next Work Duration from '")
                    .append(emailOutreachTask.getDuration())
                    .append("' to '")
                    .append(emailOutreachTaskDTO.getDuration()).append("'");
            nonStatusChangesMade = true;
        }


        //compare work date(due date) changes
        if(emailOutreachTask.getDueDate()!= null) {
            if (!emailOutreachTask.getDueDate().equals((emailOutreachTaskDTO.getDueDate()))) {
                String oldDateFormatted = Instant.ofEpochMilli(emailOutreachTask.getDueDate())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(formatter);

                String newDateFormatted = Instant.ofEpochMilli(emailOutreachTaskDTO.getDueDate())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(formatter);
                changes.append(username)
                        .append(" has changed the Next Work Date from '")
                        .append(oldDateFormatted)
                        .append("' to '")
                        .append(newDateFormatted)
                        .append("'");
                nonStatusChangesMade = true;
            }
        }

        if(emailOutreachTask.getParentTask() == null)
        {
            if(emailOutreachTaskDTO.getParentTaskId()!= null && emailOutreachTaskDTO.getParentTaskId() != 0){
                changes.append(username)
                        .append(" has updated the parent task from '")
                        .append(Objects.toString(emailOutreachTask.getParentTask(), "None"))
                        .append("' to '")
                        .append(emailOutreachTaskDTO.getParentTaskId()).append("'");
                nonStatusChangesMade = true;
            }
        }else if(emailOutreachTaskDTO.getParentTaskId()== null)
        {
            changes.append(username)
                    .append(" has updated the parent task from '")
                    .append(emailOutreachTask.getParentTask().getTaskId())
                    .append("' to '")
                    .append(emailOutreachTaskDTO.getParentTaskId()).append("'");
            nonStatusChangesMade = true;
        }
        else if(emailOutreachTask.getParentTask()!= null) {
            if (!emailOutreachTask.getParentTask().getTaskId().equals(emailOutreachTaskDTO.getParentTaskId())) {
                changes.append(username)
                        .append(" has updated the parent task from '")
                        .append(emailOutreachTask.getParentTask().getTaskId())
                        .append("' to '")
                        .append(emailOutreachTaskDTO.getParentTaskId()).append("'");
                nonStatusChangesMade = true;
            }
        }




        // 4. Handle task movement using the new path builders
        String oldPath = buildFullEmailOutreachTaskPathForComment(emailOutreachTask);
        String newPath = buildFullEmailOutreachTaskPathForComment(emailOutreachTaskDTO);
        if (!oldPath.equals(newPath)) {
            changes.append(username)
                    .append(" moved task from: ")
                    .append(oldPath)
                    .append(" to: ")
                    .append(newPath);
            nonStatusChangesMade = true;
        }

        // 5. Handle status changes
        String existingStatus = emailOutreachTask.getStatus();
        String newStatus = emailOutreachTaskDTO.getStatus();
        boolean statusChanged = !Objects.equals(existingStatus, newStatus);

        if (statusChanged) {
            changes.append(username)
                    .append(" updated Status from '")
                    .append(existingStatus != null ? existingStatus : "None")
                    .append("' to '")
                    .append(newStatus != null ? newStatus : "None")
                    .append("'");
        }

        // 6. Transform and save
        EmailOutreachTask updatedTask = transformToEmailOutreachTask(emailOutreachTaskDTO);
        Contact updatedContact = transformToContact(emailOutreachTaskDTO);
        updatedContact = contactRepository.save(updatedContact);
        updatedTask.setRelatedContact(updatedContact);
        Task savedTask = taskRepository.save(updatedTask);


        // 7. Save system comment if changes were made
        boolean isSystemComment = false;
        if (changes.length() > 0) {
            Comments comment = new Comments();
            comment.setTask(savedTask);
            comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
            comment.setCommentDescription(changes.toString());
            comment.setCreatedOn(System.currentTimeMillis());
            comment.setIsSystemComment(true);
            commentsRepository.save(comment);
            isSystemComment = true;
        }

        // 8. Return updated DTO
        EmailOutreachTaskDTO updatedDTO = transformToEmailOutreachTaskDTO((EmailOutreachTask) savedTask);
        updatedDTO.setIsSystemComment(isSystemComment);
        return updatedDTO;

    }

    @Transactional
    public LinkedInOutreachTaskDTO updateLinkedInOutreachTask(LinkedInOutreachTaskDTO linkedInOutreachTaskDTO) {

        // 1. Get and validate existing task
        Optional<Task> opt = taskRepository.findById(linkedInOutreachTaskDTO.getTaskId());
        if (!opt.isPresent()) {
            throw new IllegalArgumentException("Task does not exist");
        }

        LinkedInOutreachTask linkedInOutreachTask = (LinkedInOutreachTask) opt.get();

        StringBuilder changes = new StringBuilder();
        String username = RadiusUtil.getCurrentUsername();
        boolean nonStatusChangesMade = false;


        // Task Name
        if (linkedInOutreachTask.getTaskName() == null) {
            if (linkedInOutreachTaskDTO.getTaskName() != null) {
                changes.append(username)
                        .append(" updated the Title. Old Value: '")
                        .append(linkedInOutreachTask.getTaskName())
                        .append("'  New Value: '")
                        .append(linkedInOutreachTaskDTO.getTaskName()).append("'");
                nonStatusChangesMade = true;
            }
        } else if (linkedInOutreachTaskDTO.getTaskName() == null) {
            changes.append(username)
                    .append(" updated the Title. Old Value: '")
                    .append(linkedInOutreachTask.getTaskName())
                    .append("'  New Value: '")
                    .append(linkedInOutreachTaskDTO.getTaskName()).append("'");
            nonStatusChangesMade = true;
        } else if (!Objects.equals(linkedInOutreachTask.getTaskName(), linkedInOutreachTaskDTO.getTaskName())) {
            changes.append(username)
                    .append(" updated the Title. Old Value: '")
                    .append(linkedInOutreachTask.getTaskName())
                    .append("'  New Value: '")
                    .append(linkedInOutreachTaskDTO.getTaskName()).append("'");
            nonStatusChangesMade = true;
        }

        // Description
        if ((linkedInOutreachTask.getDescription() == null || linkedInOutreachTask.getDescription().isEmpty() || linkedInOutreachTask.getDescription().equalsIgnoreCase("No Description"))
                && linkedInOutreachTaskDTO.getDescription() != null && !linkedInOutreachTaskDTO.getDescription().isEmpty()
                && !linkedInOutreachTaskDTO.getDescription().equalsIgnoreCase("No Description")) {
            changes.append(username)
                    .append(" added a Description: ")
                    .append(linkedInOutreachTaskDTO.getDescription())
                    .append(". ");
            nonStatusChangesMade = true;

        }
        else if(linkedInOutreachTaskDTO.getDescription()== null && linkedInOutreachTask.getDescription() != null){
            changes.append(username)
                    .append(" updated the Description. Old Value: '")
                    .append(linkedInOutreachTask.getDescription())
                    .append("'  New Value: '")
                    .append(linkedInOutreachTaskDTO.getDescription()).append("'");
            nonStatusChangesMade = true;
        }
        else if (!Objects.equals(linkedInOutreachTask.getDescription(), linkedInOutreachTaskDTO.getDescription())) {
            changes.append(username)
                    .append(" updated the Description. Old Value: '")
                    .append(linkedInOutreachTask.getDescription())
                    .append("'  New Value: '")
                    .append(linkedInOutreachTaskDTO.getDescription()).append("'");
            nonStatusChangesMade = true;
        }

        // Compare AssignTo change
        if ((linkedInOutreachTask.getAssignTo() == null || linkedInOutreachTask.getAssignTo().isEmpty())
                && linkedInOutreachTaskDTO.getAssignTo() != null && !linkedInOutreachTaskDTO.getAssignTo().isEmpty()) {
            changes.append(username)
                    .append(" assigned the task to: ")
                    .append(linkedInOutreachTaskDTO.getAssignTo())
                    .append(". ");
            nonStatusChangesMade = true;

        }
        else if(linkedInOutreachTaskDTO.getAssignTo()== null){
            changes.append(username)
                    .append(" changed the Assignee from '")
                    .append(linkedInOutreachTask.getAssignTo())
                    .append("' to '")
                    .append(linkedInOutreachTaskDTO.getAssignTo()).append("'");
            nonStatusChangesMade = true;
        }
        else if (!Objects.equals(linkedInOutreachTask.getAssignTo(), linkedInOutreachTaskDTO.getAssignTo())) {
            changes.append(username)
                    .append(" changed the Assignee from '")
                    .append(linkedInOutreachTask.getAssignTo())
                    .append("' to '")
                    .append(linkedInOutreachTaskDTO.getAssignTo()).append("'");
            nonStatusChangesMade = true;

        }

        //Compare duration change(working with additional comment)
        if(linkedInOutreachTask.getDuration() == null)
        {
            if(linkedInOutreachTaskDTO.getDuration()!= null){
                changes.append(username)
                        .append(" changed the Next Work Duration from '")
                        .append(linkedInOutreachTask.getDuration())
                        .append("' to '")
                        .append(linkedInOutreachTaskDTO.getDuration()).append("'");
                nonStatusChangesMade = true;
            }
        }else if(linkedInOutreachTaskDTO.getDuration() == null && linkedInOutreachTask.getDuration() != null){
            changes.append(username)
                    .append(" changed the Next Work Duration from '")
                    .append(linkedInOutreachTask.getDuration())
                    .append("' to '")
                    .append(linkedInOutreachTaskDTO.getDuration()).append("'");
            nonStatusChangesMade = true;
        }
        else if(!Objects.equals(linkedInOutreachTask.getDuration(), linkedInOutreachTaskDTO.getDuration()))
        {
            changes.append(username)
                    .append("'  changed the Next Work Duration from '")
                    .append(linkedInOutreachTask.getDuration())
                    .append("' to '")
                    .append(linkedInOutreachTaskDTO.getDuration()).append("'");
            nonStatusChangesMade = true;
        }


        //compare work date(due date) changes
        if(linkedInOutreachTask.getDueDate()!= null) {
            if (!linkedInOutreachTask.getDueDate().equals((linkedInOutreachTaskDTO.getDueDate()))) {
                String oldDateFormatted = Instant.ofEpochMilli(linkedInOutreachTask.getDueDate())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(formatter);

                String newDateFormatted = Instant.ofEpochMilli(linkedInOutreachTaskDTO.getDueDate())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(formatter);
                changes.append(username)
                        .append(" has changed the Next Work Date from '")
                        .append(oldDateFormatted)
                        .append("' to '")
                        .append(newDateFormatted)
                        .append("'");
                nonStatusChangesMade = true;
            }
        }

        if(linkedInOutreachTask.getParentTask() == null)
        {
            if(linkedInOutreachTaskDTO.getParentTaskId()!= null && linkedInOutreachTaskDTO.getParentTaskId() != 0){
                changes.append(username)
                        .append(" has updated the parent task from '")
                        .append(Objects.toString(linkedInOutreachTask.getParentTask(), "None"))
                        .append("' to '")
                        .append(linkedInOutreachTaskDTO.getParentTaskId()).append("'");
                nonStatusChangesMade = true;
            }
        }else if(linkedInOutreachTaskDTO.getParentTaskId()== null)
        {
            changes.append(username)
                    .append(" has updated the parent task from '")
                    .append(linkedInOutreachTask.getParentTask().getTaskId())
                    .append("' to '")
                    .append(linkedInOutreachTaskDTO.getParentTaskId()).append("'");
            nonStatusChangesMade = true;
        }
        else if(linkedInOutreachTask.getParentTask()!= null) {
            if (!linkedInOutreachTask.getParentTask().getTaskId().equals(linkedInOutreachTaskDTO.getParentTaskId())) {
                changes.append(username)
                        .append(" has updated the parent task from '")
                        .append(linkedInOutreachTask.getParentTask().getTaskId())
                        .append("' to '")
                        .append(linkedInOutreachTaskDTO.getParentTaskId()).append("'");
                nonStatusChangesMade = true;
            }
        }




        // 4. Handle task movement using the new path builders
//        String oldPath = buildFullOutreachTaskPathForComment(linkedInOutreachTask);
//        String newPath = buildFullOutreachTaskPathForComment(linkedInOutreachTaskDTO);
//        if (!oldPath.equals(newPath)) {
//            changes.append(username)
//                    .append(" moved task from: ")
//                    .append(oldPath)
//                    .append(" to: ")
//                    .append(newPath);
//            nonStatusChangesMade = true;
//        }

        // 5. Handle status changes
        String existingStatus = linkedInOutreachTask.getStatus();
        String newStatus = linkedInOutreachTaskDTO.getStatus();
        boolean statusChanged = !Objects.equals(existingStatus, newStatus);

        if (statusChanged) {
            changes.append(username)
                    .append(" updated Status from '")
                    .append(existingStatus != null ? existingStatus : "None")
                    .append("' to '")
                    .append(newStatus != null ? newStatus : "None")
                    .append("'");
        }

        // 6. Transform and save
        LinkedInOutreachTask updatedTask = transformToLinkedInOutreachTask(linkedInOutreachTaskDTO);
        Contact updatedContact = transformToContact(linkedInOutreachTaskDTO);
        updatedContact = contactRepository.save(updatedContact);
        updatedTask.setRelatedContact(updatedContact);
        Task savedTask = taskRepository.save(updatedTask);


        // 7. Save system comment if changes were made
        boolean isSystemComment = false;
        if (changes.length() > 0) {
            Comments comment = new Comments();
            comment.setTask(savedTask);
            comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
            comment.setCommentDescription(changes.toString());
            comment.setCreatedOn(System.currentTimeMillis());
            comment.setIsSystemComment(true);
            commentsRepository.save(comment);
            isSystemComment = true;
        }

        // 8. Return updated DTO
        LinkedInOutreachTaskDTO updatedDTO = transformToLinkedInOutreachTaskDTO((LinkedInOutreachTask) savedTask);
        updatedDTO.setIsSystemComment(isSystemComment);
        return updatedDTO;

    }

//    private String buildFullEmailOutreachTaskPathForComment(LinkedInOutreachTask task) {
//        StringBuilder path = new StringBuilder();
//        buildOutreachLifecyclePath(task, path);
//        buildParentOutreachTaskPath(task, path);
//        path.append(" > ").append(task.getTaskName() != null ? task.getTaskName() : "Unnamed");
//
//        if (task.getLifecycle() != null && task.getLifecycle().getLifecycleName() != null) {
//            path.append(" > ").append(task.getLifecycle().getLifecycleName());
//        }
//
//        return path.toString();
//    }
//
//    private String buildFullEmailOutreachTaskPathForComment(LinkedInOutreachTaskDTO taskDTO) {
//        StringBuilder path = new StringBuilder();
//
//        // Handle lifecycle path for DTO
//        if (taskDTO.getLifecycleId() != null) {
//            Lifecycle lifecycle = lifecycleRepository.findById(taskDTO.getLifecycleId()).orElse(null);
//            if (lifecycle != null) {
//                buildOutreachLifecyclePath(lifecycle, path);
//            } else {
//                path.append("No Workspace/Campaign/Opportunity");
//            }
//        } else {
//            path.append("No Workspace/Campaign/Opportunity");
//        }
//
//        // Handle parent task path for DTO
//        if (taskDTO.getParentTaskId() != null) {
//            Task parent = taskRepository.findById(taskDTO.getParentTaskId()).orElse(null);
//            if (parent != null) {
//                List<Task> parents = new ArrayList<>();
//                Task current = parent;
//                while (current != null) {
//                    parents.add(current);
//                    current = current.getParentTask();
//                }
//                Collections.reverse(parents);
//                for (Task p : parents) {
//                    String taskType = (p instanceof EmailOutreachTask) ? "Outreach" : "Task";
//                    path.append(" > ").append(taskType).append(": ")
//                            .append(p.getTaskName() != null ? p.getTaskName() : "Unnamed");
//                }
//            }
//        }
//
//        // Add current task name
//        path.append(" > ").append(taskDTO.getTaskName() != null ? taskDTO.getTaskName() : "Unnamed");
//
//        // Add lifecycle name if exists
//        if (taskDTO.getLifecycleId() != null) {
//            Lifecycle lifecycle = lifecycleRepository.findById(taskDTO.getLifecycleId()).orElse(null);
//            if (lifecycle != null && lifecycle.getLifecycleName() != null) {
//                path.append(" > ").append(lifecycle.getLifecycleName());
//            }
//        }
//
//        return path.toString();
//    }

    private String buildFullEmailOutreachTaskPathForComment(EmailOutreachTask task) {
        StringBuilder path = new StringBuilder();
        buildOutreachLifecyclePath(task, path);
        buildParentOutreachTaskPath(task, path);
        path.append(" > ").append(task.getTaskName() != null ? task.getTaskName() : "Unnamed");

        if (task.getLifecycle() != null && task.getLifecycle().getLifecycleName() != null) {
            path.append(" > ").append(task.getLifecycle().getLifecycleName());
        }

        return path.toString();
    }

    private String buildFullEmailOutreachTaskPathForComment(EmailOutreachTaskDTO taskDTO) {
        StringBuilder path = new StringBuilder();

        // Handle lifecycle path for DTO
        if (taskDTO.getLifecycleId() != null) {
            Lifecycle lifecycle = lifecycleRepository.findById(taskDTO.getLifecycleId()).orElse(null);
            if (lifecycle != null) {
                buildOutreachLifecyclePath(lifecycle, path);
            } else {
                path.append("No Workspace/Campaign/Opportunity");
            }
        } else {
            path.append("No Workspace/Campaign/Opportunity");
        }

        // Handle parent task path for DTO
        if (taskDTO.getParentTaskId() != null) {
            Task parent = taskRepository.findById(taskDTO.getParentTaskId()).orElse(null);
            if (parent != null) {
                List<Task> parents = new ArrayList<>();
                Task current = parent;
                while (current != null) {
                    parents.add(current);
                    current = current.getParentTask();
                }
                Collections.reverse(parents);
                for (Task p : parents) {
                    String taskType = (p instanceof EmailOutreachTask) ? "Outreach" : "Task";
                    path.append(" > ").append(taskType).append(": ")
                            .append(p.getTaskName() != null ? p.getTaskName() : "Unnamed");
                }
            }
        }

        // Add current task name
        path.append(" > ").append(taskDTO.getTaskName() != null ? taskDTO.getTaskName() : "Unnamed");

        // Add lifecycle name if exists
        if (taskDTO.getLifecycleId() != null) {
            Lifecycle lifecycle = lifecycleRepository.findById(taskDTO.getLifecycleId()).orElse(null);
            if (lifecycle != null && lifecycle.getLifecycleName() != null) {
                path.append(" > ").append(lifecycle.getLifecycleName());
            }
        }

        return path.toString();
    }

    private void buildOutreachLifecyclePath(EmailOutreachTask task, StringBuilder path) {
        if (task.getLifecycle() == null) {
            path.append("No Workspace/Campaign/Opportunity");
            return;
        }
        buildOutreachLifecyclePath(task.getLifecycle(), path);
    }

    private void buildOutreachLifecyclePath(Lifecycle lifecycle, StringBuilder path) {
        Workspace workspace = null;
        Campaign campaign = null;

        if (lifecycle.getLead() != null) {
            campaign = lifecycle.getLead().getCampaign();
        } else if (lifecycle.getOpportunity() != null) {
            workspace = lifecycle.getOpportunity().getWorkspace();
        } else if (lifecycle.getCampaign() != null) {
            campaign = lifecycle.getCampaign();
        }

        if (campaign != null) {
            workspace = campaign.getWorkspace();
        }

        if (workspace != null) {
            path.append(workspace.getWorkspaceName());
        } else {
            path.append("Unknown Workspace");
        }

        if (campaign != null) {
            path.append(" > ").append(campaign.getCampaignName());
        }

        if (lifecycle.getLead() != null) {
            path.append(" > ").append(lifecycle.getLead().getLeadName() != null ?
                    lifecycle.getLead().getLeadName() : "Unnamed");
        } else if (lifecycle.getOpportunity() != null) {
            path.append(" > ").append(lifecycle.getOpportunity().getOpportunityName() != null ?
                    lifecycle.getOpportunity().getOpportunityName() : "Unnamed");
        }
    }

    private void buildParentOutreachTaskPath(EmailOutreachTask task, StringBuilder path) {
        if (task.getParentTask() == null) return;

        List<Task> parents = new ArrayList<>();
        Task current = task.getParentTask();
        while (current != null) {
            parents.add(current);
            current = current.getParentTask();
        }
        Collections.reverse(parents);
        for (Task parent : parents) {
            String taskType = (parent instanceof EmailOutreachTask) ? "Outreach" : "Task";
            path.append(" > ").append(taskType).append(": ")
                    .append(parent.getTaskName() != null ? parent.getTaskName() : "Unnamed");
        }
    }

//    private String buildFullPhoneOutreachTaskPathForComment(PhoneOutreachTask task) {
//        StringBuilder path = new StringBuilder();
//        buildOutreachLifecyclePath(task, path);
//        buildParentOutreachTaskPath(task, path);
//        path.append(" > ").append(task.getTaskName() != null ? task.getTaskName() : "Unnamed");
//
//        if (task.getLifecycle() != null && task.getLifecycle().getLifecycleName() != null) {
//            path.append(" > ").append(task.getLifecycle().getLifecycleName());
//        }
//
//        return path.toString();
//    }
//
//    private String buildFullPhoneOutreachTaskPathForComment(PhoneOutreachTaskDTO taskDTO) {
//        StringBuilder path = new StringBuilder();
//
//        if (taskDTO.getLifecycleId() != null) {
//            Lifecycle lifecycle = lifecycleRepository.findById(taskDTO.getLifecycleId()).orElse(null);
//            if (lifecycle != null) {
//                buildOutreachLifecyclePath(lifecycle, path);
//            } else {
//                path.append("No Workspace/Campaign/Opportunity");
//            }
//        } else {
//            path.append("No Workspace/Campaign/Opportunity");
//        }
//
//        if (taskDTO.getParentTaskId() != null) {
//            Task parent = taskRepository.findById(taskDTO.getParentTaskId()).orElse(null);
//            if (parent != null) {
//                List<Task> parents = new ArrayList<>();
//                Task current = parent;
//                while (current != null) {
//                    parents.add(current);
//                    current = current.getParentTask();
//                }
//                Collections.reverse(parents);
//                for (Task p : parents) {
//                    String taskType = (p instanceof PhoneOutreachTask) ? "Phone Outreach" : "Task";
//                    path.append(" > ").append(taskType).append(": ")
//                            .append(p.getTaskName() != null ? p.getTaskName() : "Unnamed");
//                }
//            }
//        }
//
//        path.append(" > ").append(taskDTO.getTaskName() != null ? taskDTO.getTaskName() : "Unnamed");
//
//        if (taskDTO.getLifecycleId() != null) {
//            Lifecycle lifecycle = lifecycleRepository.findById(taskDTO.getLifecycleId()).orElse(null);
//            if (lifecycle != null && lifecycle.getLifecycleName() != null) {
//                path.append(" > ").append(lifecycle.getLifecycleName());
//            }
//        }
//
//        return path.toString();
//    }


    private String normalize(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }
    @Transactional
    public TaskDTO updateTask(TaskDTO taskDTO)
    {
        Optional<Task> existingTaskOpt = taskRepository.findById(taskDTO.getTaskId());

        if(!existingTaskOpt.isPresent())
        {
            throw new IllegalArgumentException("task does not exist");
        }
        Task existingTask = existingTaskOpt.get();

        StringBuilder changes = new StringBuilder();

        String username = RadiusUtil.getCurrentUsername();
        // Track if any non-status changes were made
        boolean nonStatusChangesMade = false;

        // Compare name change
        if(existingTask.getTaskName() == null){
            if(taskDTO.getTaskName()!= null){
                changes.append(username)
                        .append(" updated the Title. Old Value: '")
                        .append(existingTask.getTaskName())
                        .append("'  New Value: '")
                        .append(taskDTO.getTaskName()).append("'");
                nonStatusChangesMade = true;
            }
        }else if(taskDTO.getTaskName() == null){
            changes.append(username)
                    .append(" updated the Title. Old Value: '")
                    .append(existingTask.getTaskName())
                    .append("'  New Value: '")
                    .append(taskDTO.getTaskName()).append("'");
            nonStatusChangesMade = true;
        } else if (!existingTask.getTaskName().equals(taskDTO.getTaskName())) {
            changes.append(username)
                    .append(" updated the Title. Old Value: '")
                    .append(existingTask.getTaskName())
                    .append("'  New Value: '")
                    .append(taskDTO.getTaskName()).append("'");
            nonStatusChangesMade = true;
        }

        // Compare description change
        if ((existingTask.getDescription() == null || existingTask.getDescription().isEmpty() || existingTask.getDescription().equalsIgnoreCase("No Description"))
                && taskDTO.getDescription() != null && !taskDTO.getDescription().isEmpty()
                && !taskDTO.getDescription().equalsIgnoreCase("No Description")) {
            changes.append(username)
                    .append(" added a Description: ")
                    .append(taskDTO.getDescription());
//                .append(" ");
            nonStatusChangesMade = true;

        }
        else if(taskDTO.getDescription()== null){
            changes.append(username)
                    .append(" updated the Description. Old Value: '")
                    .append(existingTask.getDescription())
                    .append("'  New Value: '")
                    .append(taskDTO.getDescription()).append("'");
            nonStatusChangesMade = true;
        }
        else if (!existingTask.getDescription().equals(taskDTO.getDescription())) {
            changes.append(username)
                    .append(" updated the Description. Old Value: '")
                    .append(existingTask.getDescription())
                    .append("'  New Value: '")
                    .append(taskDTO.getDescription()).append("'");
            nonStatusChangesMade = true;
        }

        // Compare parent Task change
        if(existingTask.getParentTask() == null)
        {
            if(taskDTO.getParentTaskId()!= null && taskDTO.getParentTaskId() != 0){
                changes.append(username)
                        .append(" has updated the parent task from '")
                        .append(Objects.toString(existingTask.getParentTask(), "None"))
                        .append("' to '")
                        .append(taskDTO.getParentTaskId()).append("'");
                nonStatusChangesMade = true;
            }
        }else if(taskDTO.getParentTaskId()== null)
        {
            changes.append(username)
                    .append(" has updated the parent task from '")
                    .append(existingTask.getParentTask().getTaskId())
                    .append("' to '")
                    .append(taskDTO.getParentTaskId()).append("'");
            nonStatusChangesMade = true;
        }
        else if(existingTask.getParentTask()!= null) {
            if (!existingTask.getParentTask().getTaskId().equals(taskDTO.getParentTaskId())) {
                changes.append(username)
                        .append(" has updated the parent task from '")
                        .append(existingTask.getParentTask().getTaskId())
                        .append("' to '")
                        .append(taskDTO.getParentTaskId()).append("'");
                nonStatusChangesMade = true;
            }
        }

        //assign to
        String oldAssignTo = normalize(existingTask.getAssignTo());
        String newAssignTo = normalize(taskDTO.getAssignTo());

        if (oldAssignTo == null && newAssignTo != null) {
            changes.append(username)
                    .append(" assigned the task to: ")
                    .append(newAssignTo)
                    .append(". ");
            nonStatusChangesMade = true;

        } else if (newAssignTo == null && oldAssignTo != null) {
            changes.append(username)
                    .append(" changed the Assignee from '")
                    .append(oldAssignTo)
                    .append("' to '")
                    .append(newAssignTo).append("'");
            nonStatusChangesMade = true;

        } else if (!Objects.equals(oldAssignTo, newAssignTo)) {
            changes.append(username)
                    .append(" changed the Assignee from '")
                    .append(oldAssignTo)
                    .append("' to '")
                    .append(newAssignTo).append("'");
            nonStatusChangesMade = true;
        }


        //Compare duration change
        if(existingTask.getDuration() == null)
        {
            if(taskDTO.getDuration()!= null){
                changes.append(username)
                        .append(" changed the Next Work Duration from '")
                        .append(existingTask.getDuration())
                        .append("' to '")
                        .append(taskDTO.getDuration()).append("'");
                nonStatusChangesMade = true;
            }
        }else if(taskDTO.getDuration() == null){
            changes.append(username)
                    .append(" changed the Next Work Duration from '")
                    .append(existingTask.getDuration())
                    .append("' to '")
                    .append(taskDTO.getDuration()).append("'");
            nonStatusChangesMade = true;
        }
        else if(!existingTask.getDuration().equals(taskDTO.getDuration()))
        {
            changes.append(username)
                    .append(" changed the Next Work Duration from '")
                    .append(existingTask.getDuration())
                    .append("' to '")
                    .append(taskDTO.getDuration()).append("'");
            nonStatusChangesMade = true;
        }

        //compare work date(due date) changes
        if(existingTask.getDueDate()!= null) {
            if (!existingTask.getDueDate().equals((taskDTO.getDueDate()))) {
                String oldDateFormatted = Instant.ofEpochMilli(existingTask.getDueDate())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(formatter);

                String newDateFormatted = Instant.ofEpochMilli(taskDTO.getDueDate())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(formatter);
                changes.append(username)
                        .append(" has changed the Next Work Date from '")
                        .append(oldDateFormatted)
                        .append("' to '")
                        .append(newDateFormatted)
                        .append("'");
                nonStatusChangesMade = true;
            }
        }

        boolean lifecycleChanged = !Objects.equals(
                existingTask.getLifecycle() != null ? existingTask.getLifecycle().getLifecycleId() : null,
                taskDTO.getLifecycleId()
        );

        boolean parentChanged = !Objects.equals(
                existingTask.getParentTask() != null ? existingTask.getParentTask().getTaskId() : null,
                taskDTO.getParentTaskId()
        );

        if (lifecycleChanged || parentChanged) {
            String oldPath = buildFullTaskPathForComment(existingTask);
            String newPath = buildFullTaskPathForComment(taskDTO);

            changes.append(username)
                    .append(" moved this task.\nFrom: ")
                    .append(oldPath)
                    .append("\nTo: ")
                    .append(newPath)
                    .append("\n");
        }

        // SendToAssignee with status update
        boolean sendToAssigneeChangedToTrue = !existingTask.isSendToAssignee() && taskDTO.isSendToAssignee();
        String existingStatus = existingTask.getStatus();
        String newStatus = taskDTO.getStatus();
        boolean statusChanged = (existingStatus == null && newStatus != null) ||
                (existingStatus != null && newStatus == null) ||
                (existingStatus != null && newStatus != null && !existingStatus.equalsIgnoreCase(newStatus));

        if (sendToAssigneeChangedToTrue) {
            changes.append(username)
                    .append(" sent the task to '")
                    .append(taskDTO.getAssignTo())
                    .append("'. ");

            if (statusChanged) {
                changes.append("Task status is updated from '")
                        .append(existingStatus != null ? existingStatus : "None")
                        .append("' to '")
                        .append(newStatus != null ? newStatus : "None")
                        .append("'. ");
            }
            statusChanged = false;
        } else if (statusChanged && !nonStatusChangesMade) {
            changes.append(username)
                    .append(" updated the Task status from '")
                    .append(existingStatus != null ? existingStatus : "None")
                    .append("' to '")
                    .append(newStatus != null ? newStatus : "None")
                    .append("'. ");
        }



        boolean isSystemComment = false;

        Task task = transformToTask(taskDTO);

        if (changes.length() > 0) {
            Comments comment = new Comments();
            comment.setTask(task);
            comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
            comment.setCommentDescription(changes.toString());
            comment.setCreatedOn(System.currentTimeMillis());
            comment.setIsSystemComment(true);
//            comment.setIsSystemComment(true);
            commentsRepository.save(comment);

            isSystemComment = true;
        }

        Task savedTask = taskRepository.save(task);

        TaskDTO updatedDTO = transformToTaskDTO(savedTask);

        updatedDTO.setIsSystemComment(isSystemComment);

        return updatedDTO;
    }


    private String buildFullTaskPathForComment(Task task) {

        String path = buildTaskPath(task);
        return path;
    }

    private String buildFullTaskPathForComment(TaskDTO taskDTO) {

        StringBuilder pathBuilder = new StringBuilder();

        // Handle lifecycle path for DTO
        if (taskDTO.getLifecycleId() != null) {
            Lifecycle lifecycle = lifecycleRepository.findById(taskDTO.getLifecycleId()).orElse(null);
            if (lifecycle != null) {

                if (lifecycle.getVersion() != null) {
                    Version v = lifecycle.getVersion();
                    Edition e = v.getEdition();
                    MarketingStory s = (e != null ? e.getMarketingStory() : null);
                    Collection c = (s != null ? s.getCollection() : null);
                    Workspace ws = (c != null ? c.getWorkspace() : null);

                    if (ws != null) {
                        pathBuilder.append(ws.getWorkspaceName()).append(" / ");
                    }
                    if (c != null) {
                        pathBuilder.append(c.getDisplayName()).append(" / ");
                    }
                    if (s != null) {
                        pathBuilder.append(s.getTitle()).append(" / ");
                    }
                    if (e != null) {
                        pathBuilder.append(e.getContentType()).append(" / ");
                    }
                    pathBuilder.append(v.getVersion());
                } else if (lifecycle.getCampaign() != null && lifecycle.getCampaign().getWorkspace() != null) {
                    pathBuilder.append(lifecycle.getCampaign().getWorkspace().getWorkspaceName())
                            .append(" / ").append(lifecycle.getCampaign().getCampaignName());
                } else if (lifecycle.getLead() != null && lifecycle.getLead().getCampaign() != null
                        && lifecycle.getLead().getCampaign().getWorkspace() != null) {
                    pathBuilder.append(lifecycle.getLead().getCampaign().getWorkspace().getWorkspaceName())
                            .append(" / ").append(lifecycle.getLead().getCampaign().getCampaignName());
                } else if (lifecycle.getOpportunity() != null && lifecycle.getOpportunity().getWorkspace() != null) {
                    pathBuilder.append(lifecycle.getOpportunity().getWorkspace().getWorkspaceName())
                            .append(" / ").append(lifecycle.getOpportunity().getOpportunityName());
                } else {
                    pathBuilder.append("unassigned");
                }

                // Lead
                if (lifecycle.getLead() != null) {
                    pathBuilder.append(" / ").append(lifecycle.getLead().getLeadName());
                }

                // Lifecycle
                pathBuilder.append(" / ").append(lifecycle.getLifecycleName());
            } else {
                pathBuilder.append("No Workspace/Campaign/Opportunity");
            }
        } else {
            pathBuilder.append("No Workspace/Campaign/Opportunity");
        }

        // Handle parent task path for DTO
        if (taskDTO.getParentTaskId() != null) {
            Task parent = taskRepository.findById(taskDTO.getParentTaskId()).orElse(null);
            if (parent != null) {
                List<Task> parents = new ArrayList<>();
                Task current = parent;
                while (current != null) {
                    parents.add(current);
                    current = current.getParentTask();
                }
                Collections.reverse(parents);
                for (Task p : parents) {
                    pathBuilder.append(" / ").append(p.getTaskName() != null ? p.getTaskName() : "Unnamed Parent Task");
                }
            }
        }

        return pathBuilder.toString();
    }

    public List<Task> getAllTask()
    {
        return taskRepository.findAll();
    }

    public List<TaskDTO> getEmailOutreachTasks() {
        return taskRepository.findAll().stream()
                .filter(task -> {
                    DiscriminatorValue dv = task.getClass().getAnnotation(DiscriminatorValue.class);
                    return dv != null && TaskType.EMAIL_OUTREACH_TASK.equalsIgnoreCase(dv.value());
                })
                .map(this::transformToTaskDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public String updateEmailOutreachTaskStatus(Long taskId, String requestedStatus) {
        boolean isSystemComment = false;
        Optional<Task> optionalTask = taskRepository.findById(taskId);
        if (optionalTask.isEmpty()) return "Task not found.";

        Task task = optionalTask.get();


        if (!(task instanceof EmailOutreachTask)) {
            return "Only Outreach tasks can use this API.";
        }

        if (task.getLifecycle() == null ||
                !LifecycleType.OUTREACH.equalsIgnoreCase(task.getLifecycle().getType())) {
            return "Task not associated with an OUTREACH lifecycle.";
        }

        //  Fetch valid OUTREACH statuses from DB
        List<ConstantLifecycle> validStages = constantLifecycleRepository.findByCycleId(5L);

        // Check if requestedStatus exists in DB (case-insensitive)
        Optional<ConstantLifecycle> matchedStageOpt = validStages.stream()
                .filter(cl -> cl.getCycleName().equalsIgnoreCase(requestedStatus))
                .findFirst();

        if (matchedStageOpt.isEmpty()) {
            return "Invalid lifecycle stage: '" + requestedStatus + "'";
        }

        String newStatusFromDB = matchedStageOpt.get().getCycleName(); // Preserve DB casing

        //  Avoid redundant update
        if (newStatusFromDB.equalsIgnoreCase(task.getStatus())) {
            return "Task is already in status '" + newStatusFromDB + "'";
        }

        // Get old status before updating
        String oldStatus = task.getStatus();


        //  SYSTEM COMMENT logic
        String username = RadiusUtil.getCurrentUsername();
        String commentText = username + " has updated the task status to '" + newStatusFromDB + "'.";

        Comments comment = new Comments();
        comment.setTask(task);
        comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
        comment.setCreatedOn(System.currentTimeMillis());
        comment.setIsSystemComment(true);
        comment.setCommentDescription(commentText);
        commentsRepository.save(comment);
        isSystemComment = true;
        // Perform actual update
        taskRepository.updateTaskStatus(taskId, newStatusFromDB);
        logger.info("________________________________________outreach task status gets updated.");

        Long campaignId = null;
        if (task.getLifecycle() != null) {
            if (task.getLifecycle().getCampaign() != null) {
                campaignId = task.getLifecycle().getCampaign().getCampaignId();
            } else if (task.getLifecycle().getLead() != null &&
                    task.getLifecycle().getLead().getCampaign() != null) {
                campaignId = task.getLifecycle().getLead().getCampaign().getCampaignId();
            }
        }
        if (campaignId == null) {
            throw new IllegalStateException("Task not linked to any campaign");
        }

        maturityService.updateMaturityReport(campaignId, oldStatus);
        logger.info("campaignId :{}", campaignId);
        logger.info("oldStatus :{}", oldStatus);

        if (LifecycleName.MEETING.equalsIgnoreCase(newStatusFromDB)) {
            try {
                marketingMeetingRegisterService.registerMeeting(task);
                logger.info("Meeting registered successfully for taskId: {}", taskId);
            } catch (Exception e) {
                logger.error("Failed to register meeting for taskId: {}", taskId, e);
            }
        }

        return "Task status updated to '" + newStatusFromDB + "' successfully.";
    }

    @Transactional
    public String updateLinkedInOutreachTaskStatus(Long taskId, String requestedStatus) {
        boolean isSystemComment = false;

        Optional<Task> optionalTask = taskRepository.findById(taskId);
        if (optionalTask.isEmpty()) return "Task not found.";

        Task task = optionalTask.get();

        // Validate LinkedIn outreach task type
        if (!(task instanceof LinkedInOutreachTask)) {
            return "Only LinkedIn Outreach tasks can use this API.";
        }

        // Validate OUTREACH lifecycle
        if (task.getLifecycle() == null ||
                !LifecycleType.OUTREACH.equalsIgnoreCase(task.getLifecycle().getType())) {
            return "Task not associated with an OUTREACH lifecycle.";
        }

        // Fetch valid OUTREACH statuses from DB
        List<ConstantLifecycle> validStages = constantLifecycleRepository.findByCycleId(10L);

        // Check if requestedStatus exists in DB (case-insensitive)
        Optional<ConstantLifecycle> matchedStageOpt = validStages.stream()
                .filter(cl -> cl.getCycleName().equalsIgnoreCase(requestedStatus))
                .findFirst();

        if (matchedStageOpt.isEmpty()) {
            return "Invalid lifecycle stage: '" + requestedStatus + "'";
        }

        String newStatusFromDB = matchedStageOpt.get().getCycleName(); // Preserve DB casing

        // Avoid redundant update
        if (newStatusFromDB.equalsIgnoreCase(task.getStatus())) {
            return "Task is already in status '" + newStatusFromDB + "'";
        }

        String oldStatus = task.getStatus();

        // SYSTEM COMMENT logic
        String username = RadiusUtil.getCurrentUsername();
        String commentText = username + " has updated the LinkedIn task status to '" + newStatusFromDB + "'.";

        Comments comment = new Comments();
        comment.setTask(task);
        comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
        comment.setCreatedOn(System.currentTimeMillis());
        comment.setIsSystemComment(true);
        comment.setCommentDescription(commentText);
        commentsRepository.save(comment);
        isSystemComment = true;

        // Perform actual update
        taskRepository.updateTaskStatus(taskId, newStatusFromDB);
        logger.info("LinkedIn outreach task status updated.");

        // Update maturity report
        Long campaignId = null;
        if (task.getLifecycle() != null) {
            if (task.getLifecycle().getCampaign() != null) {
                campaignId = task.getLifecycle().getCampaign().getCampaignId();
            } else if (task.getLifecycle().getLead() != null &&
                    task.getLifecycle().getLead().getCampaign() != null) {
                campaignId = task.getLifecycle().getLead().getCampaign().getCampaignId();
            }
        }
        if (campaignId == null) {
            throw new IllegalStateException("Task not linked to any campaign");
        }

        linkedInOutreachCampaignMaturityRecordRegisterService.updateMaturityReport(campaignId, oldStatus);
        logger.info("campaignId :{}", campaignId);
        logger.info("oldStatus :{}", oldStatus);

        if (LifecycleName.WAITING_FOR_ACCEPTANCE.equalsIgnoreCase(oldStatus)
                && LifecycleName.INTRO.equalsIgnoreCase(newStatusFromDB)) {

            updateMaturityForLinkedInOutreach(task, LifecycleName.ACCEPTED);
        }

        if (LifecycleName.MEETING.equalsIgnoreCase(newStatusFromDB)) {
            try {
                marketingMeetingRegisterService.registerMeeting(task);
                logger.info("Meeting registered successfully for taskId: {}", taskId);
            } catch (Exception e) {
                logger.error("Failed to register meeting for taskId: {}", taskId, e);
            }
        }

        return "LinkedIn Outreach task status updated to '" + newStatusFromDB + "' successfully.";
    }

    @Transactional
    public String updatePhoneOutreachTaskStatus(Long taskId, String requestedStatus) {
        return updatePhoneOutreachTaskStatus(taskId, requestedStatus, false);
    }

    @Transactional
    public String updatePhoneOutreachTaskStatus(Long taskId, String requestedStatus,boolean skipMaturityUpdate) {
        boolean isSystemComment = false;

        Optional<Task> optionalTask = taskRepository.findById(taskId);
        if (optionalTask.isEmpty()) return "Task not found.";

        Task task = optionalTask.get();

        // Validate LinkedIn outreach task type
        if (!(task instanceof PhoneOutreachTask)) {
            return "Only phone Outreach tasks can use this API.";
        }

        // Validate OUTREACH lifecycle
        if (task.getLifecycle() == null ||
                !LifecycleType.OUTREACH.equalsIgnoreCase(task.getLifecycle().getType())) {
            return "Task not associated with an OUTREACH lifecycle.";
        }

        // Fetch valid OUTREACH statuses from DB
        List<ConstantLifecycle> validStages = constantLifecycleRepository.findByCycleId(11L);

        // Check if requestedStatus exists in DB (case-insensitive)
        Optional<ConstantLifecycle> matchedStageOpt = validStages.stream()
                .filter(cl -> cl.getCycleName().equalsIgnoreCase(requestedStatus))
                .findFirst();

        if (matchedStageOpt.isEmpty()) {
            return "Invalid lifecycle stage: '" + requestedStatus + "'";
        }

        String newStatusFromDB = matchedStageOpt.get().getCycleName(); // Preserve DB casing

        // Avoid redundant update
        if (newStatusFromDB.equalsIgnoreCase(task.getStatus())) {
            return "Task is already in status '" + newStatusFromDB + "'";
        }

        String oldStatus = task.getStatus();

        // SYSTEM COMMENT logic
        String username = RadiusUtil.getCurrentUsername();
        String commentText = username + " has updated the phone task status to '" + newStatusFromDB + "'.";

        Comments comment = new Comments();
        comment.setTask(task);
        comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
        comment.setCreatedOn(System.currentTimeMillis());
        comment.setIsSystemComment(true);
        comment.setCommentDescription(commentText);
        commentsRepository.save(comment);
        isSystemComment = true;

        // Perform actual update
//        taskRepository.updateTaskStatus(taskId, newStatusFromDB);
        task.setStatus(newStatusFromDB);
        taskRepository.save(task);

        logger.info("phone outreach task status updated.");

        if(!skipMaturityUpdate){
            // Update maturity report
            Long campaignId = null;
            if (task.getLifecycle() != null) {
                if (task.getLifecycle().getCampaign() != null) {
                    campaignId = task.getLifecycle().getCampaign().getCampaignId();
                } else if (task.getLifecycle().getLead() != null &&
                        task.getLifecycle().getLead().getCampaign() != null) {
                    campaignId = task.getLifecycle().getLead().getCampaign().getCampaignId();
                }
            }
            if (campaignId == null) {
                throw new IllegalStateException("Task not linked to any campaign");
            }
            logger.info("Dropdown selected status :{}", newStatusFromDB);
//            phoneOutreachCampaignMaturityRecordRegisterService.updateMaturityReport(campaignId, oldStatus);
            if (!LifecycleName.NOT_INTERESTED.equalsIgnoreCase(newStatusFromDB)
//                    && !LifecycleName.MEETING.equalsIgnoreCase(newStatusFromDB)
                    && !LifecycleName.INCORRECT_NUMBER.equalsIgnoreCase(newStatusFromDB)) {

//                phoneOutreachCampaignMaturityRecordRegisterService
//                        .updateMaturityReport(campaignId, oldStatus);
                if (LifecycleName.CALLING.equalsIgnoreCase(oldStatus)) {
                    phoneCallingRegisterService.updateMaturityReport(campaignId, oldStatus);
                } else {
                    phoneOutreachCampaignMaturityRecordRegisterService.updateMaturityReport(campaignId, oldStatus);
                }

                logger.info("Calling count updated for campaignId :{}", campaignId);


            } else {
                logger.info("Calling count NOT updated for status :{}", newStatusFromDB);
            }

            logger.info("campaignId :{}", campaignId);
            logger.info("oldStatus :{}", oldStatus);

            if (LifecycleName.MEETING.equalsIgnoreCase(newStatusFromDB)) {
                try {
                    marketingMeetingRegisterService.registerMeeting(task);
                    logger.info("Meeting registered successfully for taskId: {}", taskId);
                } catch (Exception e) {
                    logger.error("Failed to register meeting for taskId: {}", taskId, e);
                }
            }

            // Additional update for COMPLETED status(because we normally update count of old status but completed is last and we want count for that as well.)
            //this for when user select meeting from dropdown
//            if (newStatusFromDB.equalsIgnoreCase(LifecycleName.COMPLETED)) {
//                logger.info("Task moved to COMPLETED — updating maturity report for COMPLETED status as well.");
//                phoneOutreachCampaignMaturityRecordRegisterService.updateMaturityReport(campaignId, LifecycleName.COMPLETED);
//            }

            //for when user select Not Interested or Incorrect Number
//            if (newStatusFromDB.equalsIgnoreCase(LifecycleName.STOPPED)) {
//                logger.info("Task moved to COMPLETED — updating maturity report for COMPLETED status as well.");
//                phoneOutreachCampaignMaturityRecordRegisterService.updateMaturityReport(campaignId, LifecycleName.STOPPED);
//            }
        }


        return "Phone Outreach task status updated to '" + newStatusFromDB + "' successfully.";
    }

    @Transactional
    public String promotionAutomationForPhoneOutreach(Long taskId, String promotionAutomationPhoneOutreachStatus) {

        String status = promotionAutomationPhoneOutreachStatus;

        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return "Task not found.";
        }
        Task task = taskOpt.get();
        Long campaignId = null;
        if (task.getLifecycle().getCampaign() != null) {
            campaignId = task.getLifecycle().getCampaign().getCampaignId();
        } else if (task.getLifecycle().getLead() != null &&
                task.getLifecycle().getLead().getCampaign() != null) {
            campaignId = task.getLifecycle().getLead().getCampaign().getCampaignId();
        }

        //Always increment "CALLING" count when user selects values from dropdown.
        phoneCallingRegisterService.updateMaturityReport(campaignId, LifecycleName.CALLING);

        // ADD COMMENT when user selects from dropdown
        String username = RadiusUtil.getCurrentUsername();
        String commentText = username + " has commented '" + status;

        Comments comment = new Comments();
        comment.setTask(task);
        comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
        comment.setCreatedOn(System.currentTimeMillis());
        comment.setIsSystemComment(true);
        comment.setCommentDescription(commentText);
        commentsRepository.save(comment);

        // CASE 1: not reachable / didn’t answer / call back later → ask user for next date
        if (status.equalsIgnoreCase(LifecycleName.CALL_BACK_LATER)) {

            return "Please ask user to provide next work date and call updatePhoneOutreachTask() with updated due date.";
        }

        // CASE 2: meeting → promote to MEETING, then to COMPLETED
        else if (status.equalsIgnoreCase(LifecycleName.MEETING)) {

            if (LifecycleName.MEETING.equalsIgnoreCase(promotionAutomationPhoneOutreachStatus)) {
                try {
                    marketingMeetingRegisterService.registerMeeting(task);
                    logger.info("Meeting registered successfully from promotion automation for taskId: {}", taskId);
                } catch (Exception e) {
                    logger.error("Failed to register meeting for taskId: {}", taskId, e);
                }
            }

            String msg1 = updatePhoneOutreachTaskStatus(taskId, LifecycleName.MEETING,true);

            String msg2 = updatePhoneOutreachTaskStatus(taskId, LifecycleName.COMPLETED);

            return msg1 + " Then, " + msg2;
        }

        // CASE 3: not interested / incorrect number → promote to STOPPED, then COMPLETED
        else if (status.equalsIgnoreCase(LifecycleName.NOT_INTERESTED) ||
                status.equalsIgnoreCase(LifecycleName.INCORRECT_NUMBER)) {

            String msg1 = updatePhoneOutreachTaskStatus(taskId, LifecycleName.STOPPED,true);

            String msg2 = updatePhoneOutreachTaskStatus(taskId, LifecycleName.COMPLETED);

            return msg1 + " Then, " + msg2;
        }

        //CASE 4: not reachable and did not answer then promote to next working date.
        else if (status.equalsIgnoreCase(LifecycleName.DIDNOT_ANSWER) ||
                status.equalsIgnoreCase(LifecycleName.NOT_REACHABLE)) {

            PhoneOutreachTask phoneOutreachTask = (PhoneOutreachTask) task;

            if (phoneOutreachTask.getDidNotAnswer() == null) {
                phoneOutreachTask.setDidNotAnswer(0L);
            }
            if (phoneOutreachTask.getNotReachable() == null) {
                phoneOutreachTask.setNotReachable(0L);
            }

            if (status.equalsIgnoreCase(LifecycleName.DIDNOT_ANSWER)) {
                phoneOutreachTask.setDidNotAnswer(phoneOutreachTask.getDidNotAnswer() + 1);
            } else if (status.equalsIgnoreCase(LifecycleName.NOT_REACHABLE)) {
                phoneOutreachTask.setNotReachable(phoneOutreachTask.getNotReachable() + 1);
            }

            long today = System.currentTimeMillis();
            long baseDate = Math.max(today, task.getDueDate());

            long nextDate =
                    calculateNextWorkingDateMillisForEmailOutreach(baseDate, 2);

            task.setDueDate(nextDate);
            taskRepository.save(task);

            return "Task due date updated to next working day: " + Instant.ofEpochMilli(nextDate);

        }

        // CASE 4: invalid status
        else {
            return "Invalid promotionAutomationPhoneOutreachStatus: " + status;
        }
    }


    public List<Task> getTaskByLifecycleId(String userName, Long lifecycleId) {
        List<Task> taskList = taskRepository.findByLifecycleIdWithSubtasks(lifecycleId);

        List<UserTaskSequence> userTaskSequences = userTaskSequenceRepository.findByUserName(userName);

        // Create a map of taskId -> sequenceOrder
        Map<Long, Integer> userSpecificSequences = userTaskSequences.stream()
                .collect(Collectors.toMap(
                        seq -> seq.getTask().getTaskId(),
                        UserTaskSequence::getSequenceOrder
                ));

        // Filter parent tasks only
        List<Task> parentTasks = taskList.stream()
                .filter(task -> task.getParentTask() == null)
                .sorted((c1, c2) -> {
                    int seq1 = userSpecificSequences.getOrDefault(c1.getTaskId(), getDefaultSequenceOrder());
                    int seq2 = userSpecificSequences.getOrDefault(c2.getTaskId(), getDefaultSequenceOrder());
                    return Integer.compare(seq1, seq2);
                })
                .collect(Collectors.toList());

        // Sort subtasks for each parent task
        for (Task parent : parentTasks) {
            List<Task> sortedSubtasks = parent.getSubTasks().stream()
                    .sorted((s1, s2) -> {
                        int seq1 = userSpecificSequences.getOrDefault(s1.getTaskId(), getDefaultSequenceOrder());
                        int seq2 = userSpecificSequences.getOrDefault(s2.getTaskId(), getDefaultSequenceOrder());
                        return Integer.compare(seq1, seq2);
                    })
                    .collect(Collectors.toList());
            parent.setSubTasks(sortedSubtasks); // Update the parent with sorted children
        }

        return parentTasks;
    }

    public int getDefaultSequenceOrder() {
        return 999;
    }


    public Optional<Task> getTaskById(Long id)
    {
        return taskRepository.findById(id);
    }

    public boolean deleteTask(Long id)
    {
        if(taskRepository.existsById(id))
        {
            taskRepository.deleteById(id);
            return true;
        }
        return false;
    }


    public String updateSendToAssigneeByTaskId(Long taskId, Boolean sendToAssignee) {
        try {
            Optional<Task> taskOptional = taskRepository.findById(taskId);
            if (taskOptional.isEmpty()) {
                return "Task not found.";
            }

            Task task = taskOptional.get();

            // Update the sendToAssignee flag
            taskRepository.updateSendToAssignee(taskId, sendToAssignee);

            if (sendToAssignee) {
                String taskType;
                Long cycleId;

                //  Determine task type and cycleId dynamically
                if (task instanceof EmailOutreachTask) {
                    taskType = TaskType.EMAIL_OUTREACH_TASK;
                    cycleId = 5L;  // Email outreach
                } else if (task instanceof PhoneOutreachTask) {
                    taskType = TaskType.PHONE_OUTREACH_TASK;
                    cycleId = 11L; // Phone outreach
                } else if (task instanceof LinkedInOutreachTask) {
                    taskType = TaskType.LINKEDIN_OUTREACH_TASK;
                    cycleId = 10L; // LinkedIn outreach
                } else {
                    taskType = TaskType.DEFAULT;
                    cycleId = 6L; // Default tasks
                }

                //  Fetch valid stages for that task type
                List<ConstantLifecycle> stages = constantLifecycleRepository.findByCycleId(cycleId);

                if (stages == null || stages.size() < 2) {
                    return "Insufficient lifecycle stages defined for type: " + taskType;
                }

                //  Move to 2nd stage (index 1)
                String secondStageStatus = stages.get(1).getCycleName();

                int rowsUpdated = taskRepository.updateTaskStatus(taskId, secondStageStatus);
                logger.info("Updated {} rows for Task ID {} with status: {}", rowsUpdated, taskId, secondStageStatus);
            }
            return "SendToAssignee and status updated successfully.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    //reschedule task api
    public String updateDueDateAndWeeklyTaskSequenceByTaskId(List<TaskWeeklyPlannerDTO> taskDTOList) {
        try {
            String username = RadiusUtil.getCurrentUsername();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE dd-MMM-yyyy");

            for (TaskWeeklyPlannerDTO taskDTO : taskDTOList) {
                // Fetch the existing task before updating
                Optional<Task> taskOptional = taskRepository.findById(taskDTO.getTaskId());

                if (taskOptional.isPresent()) {
                    Task task = taskOptional.get();
                    Long oldDueDateMillis = task.getDueDate();

                    // Update due date and weekly task sequence
                    taskRepository.updateDueDateAndWeeklyTaskSequence(
                            taskDTO.getTaskId(),
                            taskDTO.getDueDate(),
                            taskDTO.getWeeklyTaskSequence()
                    );

                    // Format old and new due dates
                    String oldDateFormatted = oldDueDateMillis != null
                            ? Instant.ofEpochMilli(oldDueDateMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
                            : "None";
                    String newDateFormatted = taskDTO.getDueDate() != null
                            ? Instant.ofEpochMilli(taskDTO.getDueDate()).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
                            : "None";

                    // Build simple human-readable comment
                    String commentText = username + " has rescheduled the task from '" + oldDateFormatted + "' to '" + newDateFormatted + "'.";

                    // Create and save system comment
                    Comments comment = new Comments();
                    comment.setTask(task);
                    comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
                    comment.setCommentDescription(commentText);
                    comment.setCreatedOn(System.currentTimeMillis());
                    comment.setIsSystemComment(true);

                    commentsRepository.save(comment);
                }
            }
            return "Due date updated successfully.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }




    public String updateAssignToAndWeeklyTaskSequenceByTaskId(Long taskId,String assignTo,int weeklyTaskSequence){
        try{
            Optional<Task> taskOptional = taskRepository.findById(taskId);
            if (taskOptional.isPresent()) {
                StringBuilder changes = new StringBuilder();
                String username = RadiusUtil.getCurrentUsername();
                changes.append(username)
                        .append("' reassigned the task to '")
                        .append(assignTo).append("'");
                Boolean isSystemComment = false;
                if (!changes.isEmpty()) {

                    Comments comment = new Comments();
                    comment.setTask(taskOptional.get());
//                    comment.setCreatedBy(username);
                    comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
                    comment.setCommentDescription(changes.toString());
                    comment.setCreatedOn(System.currentTimeMillis());

                    comment.setIsSystemComment(true);

                    commentsRepository.save(comment);
                    isSystemComment = true;
                    taskRepository.updateAssignToAndWeeklyTaskSequence(taskId, assignTo, weeklyTaskSequence);
                    return "assignTo field updated Successfully.";
                }
            }
            return "Task not Found";
        }
        catch (Exception e)
        {
            return "Error = "+e.getMessage();
        }
    }


    public String updateTaskStatus(Long taskId, String targetStatus) {
        try {
            Optional<Task> taskOptional = taskRepository.findById(taskId);
            if (taskOptional.isEmpty()) {
                return "Task not found.";
            }

            Task task = taskOptional.get();

            // Determine the type of task and corresponding cycle ID
            String taskType = task instanceof EmailOutreachTask ? TaskType.EMAIL_OUTREACH_TASK : TaskType.DEFAULT;
            Long expectedCycleId = TaskType.EMAIL_OUTREACH_TASK.equalsIgnoreCase(taskType) ? 5L : 6L;

            // Fetch valid statuses from DB for this task type
            List<ConstantLifecycle> stages = constantLifecycleRepository.findByCycleId(expectedCycleId);
            logger.info("All stages for cycle {}: {}", expectedCycleId,
                    stages.stream().map(ConstantLifecycle::getCycleName).collect(Collectors.toList()));

            if (stages.isEmpty()) {
                return "No lifecycle stages defined for type: " + taskType;
            }

            // Validate if target status exists in the allowed lifecycle
            boolean isValid = stages.stream()
                    .anyMatch(stage -> stage.getCycleName().equalsIgnoreCase(targetStatus));
            if (!isValid) {
                return "Invalid status '" + targetStatus + "' for task type: " + taskType;
            }

            // Log system comment for status change
            String username = RadiusUtil.getCurrentUsername();
            StringBuilder changes = new StringBuilder();
            changes.append(username)
                    .append(" changed status from '")
                    .append(task.getStatus())
                    .append("' to '")
                    .append(targetStatus)
                    .append("'.");

            Comments comment = new Comments();
            comment.setTask(task);
            comment.setCreatedBy(RadiusConstants.SYSTEM_USER);
            comment.setCommentDescription(changes.toString());
            comment.setCreatedOn(System.currentTimeMillis());
            comment.setIsSystemComment(true);
            commentsRepository.save(comment);

            // Perform actual status update
            taskRepository.updateTaskStatus(taskId, targetStatus);
            return "Status updated to '" + targetStatus + "' successfully.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }


    // Common helper (you can make this a private method in service if needed)
    private String getStageByIndex(List<ConstantLifecycle> stages, int index, String taskType) {
        if (index >= 0 && index < stages.size()) {
            return stages.get(index).getCycleName();
        } else {
            throw new IllegalArgumentException("Lifecycle stage at index " + index + " not found for type: " + taskType);
        }
    }


    public List<Task> findTasksByAssignToAndStatusAndBeforeGivenDate(List <String> assignTo,List <String> status,Long dueDate){
        return taskRepository.findTasksByAssignToListAndDueDate(assignTo,status,dueDate);
    }

    public String updateMultipleTasksDueDates(List<Long> taskIds, Long dueDate) {
        try {
            taskCustomRepository.bulkUpdateDueDates(taskIds, dueDate);
            return " updated Successfully";
        }catch(Exception e){
            return "error ="+ e.getMessage();
        }
    }

    public Map<String, Long> getDailyTaskCount(String assignTo, boolean sendToAssignee, int year, int month, int day) {
        List<TaskWeeklyPlannerDTO> tasks = getDailyTasks(assignTo, sendToAssignee, year, month, day,false);

        long defaultTaskCount = 0;
        long emailOutreachCount = 0;
        long linkedInOutreachCount = 0;
        long phoneOutreachCount = 0;

        for (TaskWeeklyPlannerDTO t : tasks) {
            if (t.getType() == TaskType.DEFAULT) {
                defaultTaskCount++;
            } else if (t.getType() == TaskType.EMAIL_OUTREACH_TASK) {
                emailOutreachCount++;
            } else if (t.getType() == TaskType.LINKEDIN_OUTREACH_TASK) {
                if (!"Waiting For Acceptance".equalsIgnoreCase(t.getStatus())) {
                    linkedInOutreachCount++;
                }
            } else if (t.getType() == TaskType.PHONE_OUTREACH_TASK) {
                phoneOutreachCount++;
            }
        }

        Map<String, Long> result = new HashMap<>();
        result.put("task", defaultTaskCount);
        result.put("emailOutreach", emailOutreachCount);
        result.put("linkedInOutreach", linkedInOutreachCount);
        result.put("phoneOutreach", phoneOutreachCount);

        return result;
    }

    public List<InWorkOutreachTaskTypeDTO> getInWorkEmailOutreachTasks( String assignTo,
                                                                        boolean sendToAssignee,
                                                                        int year,
                                                                        int month,
                                                                        int day) {

        //  Get all tasks (today and before today) for this user using your existing daily method
        List<TaskWeeklyPlannerDTO> dailyTasks = getDailyTasks(assignTo, sendToAssignee, year, month, day,false);

        if (dailyTasks == null || dailyTasks.isEmpty()) {
            return Collections.emptyList();
        }

        List<Task> allTasks = dailyTasks.stream()
                .map(dto -> taskRepository.findById(dto.getTaskId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return allTasks.stream()
                .filter(task -> task instanceof EmailOutreachTask)
                .filter(task -> task.getAssignTo() != null && task.getAssignTo().equalsIgnoreCase(assignTo))
                .filter(task -> task.getStatus() != null)
                .filter(task -> {
                    String status = task.getStatus();
                    return !status.equals(LifecycleName.NOT_STARTED)
                            && !status.equals(LifecycleName.COMPLETED);
                })
                .map(task -> {
                    Lifecycle lifecycle = task.getLifecycle();
                    Campaign campaign = null;

                    if (lifecycle != null) {
                        if (lifecycle.getCampaign() != null) {
                            campaign = lifecycle.getCampaign();
                        } else if (lifecycle.getLead() != null && lifecycle.getLead().getCampaign() != null) {
                            campaign = lifecycle.getLead().getCampaign();
                        }
                    }

                    return new InWorkOutreachTaskTypeDTO(
                            campaign != null ? campaign.getCampaignId() : null,
                            campaign != null ? campaign.getCampaignName() : "N/A",
                            task.getStatus(),
                            null
                    );
                })
                // Collect into a Map to remove duplicates by campaignId + lifecycleStatus
                .collect(Collectors.toMap(
                        dto -> dto.getCampaignId() + "_" + dto.getLifecycleStatus(), // unique key
                        dto -> dto, // value
                        (existing, replacement) -> existing // in case of duplicate, keep the first
                ))
                .values() // take only the unique DTOs
                .stream()
                .collect(Collectors.toList());
    }


    public List<InWorkOutreachTaskTypeDTO> getInWorkLinkedInOutreachTasks(String assignTo,
                                                                          boolean sendToAssignee,
                                                                          int year,
                                                                          int month,
                                                                          int day) {

        //  Get all tasks (today and before today) for this user using your existing daily method
        List<TaskWeeklyPlannerDTO> dailyTasks = getDailyTasks(assignTo, sendToAssignee, year, month, day,false);

        if (dailyTasks == null || dailyTasks.isEmpty()) {
            return Collections.emptyList();
        }

        List<Task> allTasks = dailyTasks.stream()
                .map(dto -> taskRepository.findById(dto.getTaskId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


        return allTasks.stream()
                .filter(task -> task instanceof LinkedInOutreachTask)
                .filter(task -> task.getAssignTo() != null && task.getAssignTo().equalsIgnoreCase(assignTo))
                .filter(task -> task.getStatus() != null)
                .filter(task -> {
                    String status = task.getStatus();
                    return !status.equals(LifecycleName.NOT_STARTED)
                            && !status.equals(LifecycleName.COMPLETED)
                            && !status.equals(LifecycleName.WAITING_FOR_ACCEPTANCE);
                })
                .map(task -> {
                    Lifecycle lifecycle = task.getLifecycle();
                    Campaign campaign = null;

                    if (lifecycle != null) {
                        if (lifecycle.getCampaign() != null) {
                            campaign = lifecycle.getCampaign();
                        } else if (lifecycle.getLead() != null && lifecycle.getLead().getCampaign() != null) {
                            campaign = lifecycle.getLead().getCampaign();
                        }
                    }

                    return new InWorkOutreachTaskTypeDTO(
                            campaign != null ? campaign.getCampaignId() : null,
                            campaign != null ? campaign.getCampaignName() : "N/A",
                            task.getStatus(),
                            null
                    );
                })
                // remove duplicates by (campaignId + lifecycleStatus)
                .collect(Collectors.toMap(
                        dto -> dto.getCampaignId() + "_" + dto.getLifecycleStatus(),
                        dto -> dto,
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    public List<EmailOutreachTaskDTO> getInWorkEmailOutreachTasksByCampaignIdAndLifecycleStatus(
            Long campaignId,
            String lifecycleStatus,
            String assignTo,
            boolean sendToAssignee,
            int year,
            int month,
            int day
    ) {

        List<TaskWeeklyPlannerDTO> dailyTasks = getDailyTasks(assignTo, sendToAssignee, year, month, day,false);
        Set<Long> validTaskIds = dailyTasks.stream()
                .map(TaskWeeklyPlannerDTO::getTaskId)
                .collect(Collectors.toSet());


        return taskRepository.findByStatusIgnoreCase(lifecycleStatus).stream()
                // Only EmailOutreachTask
                .filter(t -> t instanceof EmailOutreachTask)
                .map(t -> (EmailOutreachTask) t)
                // ❗Only tasks that appear in getDailyTasks (today or before)
                .filter(t -> validTaskIds.contains(t.getTaskId()))
                // Only tasks whose lifecycle is linked to a Lead under the given campaign
                .filter(t -> {
                    Lifecycle lifecycle = t.getLifecycle();
                    if (lifecycle == null) return false;
                    Lead lead = lifecycle.getLead();
                    return lead != null
                            && lead.getCampaign() != null
                            && lead.getCampaign().getCampaignId().equals(campaignId);
                })
                // Filter by assigned user name
                .filter(t -> t.getAssignTo() != null && t.getAssignTo().equalsIgnoreCase(assignTo))
                // Map to DTO
                .map(this::transformToEmailOutreachTaskDTO)
                .collect(Collectors.toList());
    }

    public List<LinkedInOutreachTaskDTO> getInWorkLinkedInOutreachTasksByCampaignIdAndLifecycleStatus(
            Long campaignId,
            String lifecycleStatus,
            String assignTo,
            boolean sendToAssignee,
            int year,
            int month,
            int day
    ) {

        List<TaskWeeklyPlannerDTO> dailyTasks = getDailyTasks(assignTo, sendToAssignee, year, month, day,false);
        Set<Long> validTaskIds = dailyTasks.stream()
                .map(TaskWeeklyPlannerDTO::getTaskId)
                .collect(Collectors.toSet());

        return taskRepository.findByStatusIgnoreCase(lifecycleStatus).stream()
                // Only LinkedInOutreachTask
                .filter(t -> t instanceof LinkedInOutreachTask)
                .map(t -> (LinkedInOutreachTask) t)
                // ❗Only tasks that appear in getDailyTasks (today or before)
                .filter(t -> validTaskIds.contains(t.getTaskId()))
                // Only tasks whose lifecycle is linked to a Lead under the given campaign
                .filter(t -> {
                    Lifecycle lifecycle = t.getLifecycle();
                    if (lifecycle == null) return false;
                    Lead lead = lifecycle.getLead();
                    return lead != null
                            && lead.getCampaign() != null
                            && lead.getCampaign().getCampaignId().equals(campaignId);
                })
                // Filter by assigned user name
                .filter(t -> t.getAssignTo() != null && t.getAssignTo().equalsIgnoreCase(assignTo))
                // Map to DTO
                .map(this::transformToLinkedInOutreachTaskDTO)
                .collect(Collectors.toList());
    }

    public List<InWorkOutreachTaskTypeDTO> getInWorkPhoneOutreachTasks(String assignTo,
                                                                       boolean sendToAssignee,
                                                                       int year,
                                                                       int month,
                                                                       int day) {

        //  Get all tasks (today and before today) for this user using your existing daily method
        List<TaskWeeklyPlannerDTO> dailyTasks = getDailyTasks(assignTo, sendToAssignee, year, month, day,false);

        if (dailyTasks == null || dailyTasks.isEmpty()) {
            return Collections.emptyList();
        }

        List<Task> allTasks = dailyTasks.stream()
                .map(dto -> taskRepository.findById(dto.getTaskId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


        // Step 2: Filter only PhoneOutreachTasks for the given user
        List<PhoneOutreachTask> outreachTasks = allTasks.stream()
                .filter(task -> task instanceof PhoneOutreachTask)
                .map(task -> (PhoneOutreachTask) task)
                .filter(task -> task.getAssignTo() != null && task.getAssignTo().trim().equalsIgnoreCase(assignTo.trim()))
                .filter(task -> task.getStatus() != null)
                .filter(task -> !task.getStatus().equalsIgnoreCase(LifecycleName.COMPLETED))
                .filter(task -> !task.getStatus().equalsIgnoreCase(LifecycleName.NOT_STARTED))
                .collect(Collectors.toList());

        // Step 3: Group by campaign (include Lead → Campaign)
        Map<Long, List<PhoneOutreachTask>> groupedByCampaign = outreachTasks.stream()
                .filter(task -> {
                    Lifecycle lifecycle = task.getLifecycle();
                    return lifecycle != null &&
                            (lifecycle.getCampaign() != null ||
                                    (lifecycle.getLead() != null && lifecycle.getLead().getCampaign() != null));
                })
                .collect(Collectors.groupingBy(task -> {
                    Lifecycle lifecycle = task.getLifecycle();
                    if (lifecycle.getCampaign() != null) {
                        return lifecycle.getCampaign().getCampaignId();
                    } else {
                        return lifecycle.getLead().getCampaign().getCampaignId();
                    }
                }));

        // Step 4: Build response DTO
        List<InWorkOutreachTaskTypeDTO> responseList = new ArrayList<>();

        groupedByCampaign.forEach((campaignId, tasks) -> {
            Lifecycle lifecycle = tasks.get(0).getLifecycle();
            String campaignName;

            if (lifecycle.getCampaign() != null) {
                campaignName = lifecycle.getCampaign().getCampaignName();
            } else {
                campaignName = lifecycle.getLead().getCampaign().getCampaignName();
            }

            // Map each task to inner DTO
            List<getInWorkPhoneOutreachTasksDTO> taskDTOs = tasks.stream()
                    .map(t -> new getInWorkPhoneOutreachTasksDTO(
                            t.getTaskId(),
                            t.getTaskName(),
                            t.getDueDate() != null ? t.getDueDate():null
                    ))
                    .collect(Collectors.toList());

            responseList.add(new InWorkOutreachTaskTypeDTO(
                    campaignId,
                    campaignName,
                    null,
                    taskDTOs
            ));
        });

        return responseList;
    }

    public PhoneOutreachTaskDTO getPhoneTaskDetailsByTaskId(Long taskId) {
        Optional<Task> optionalTask = taskRepository.findById(taskId);

        if (optionalTask.isEmpty()) {
            return null;
        }

        Task task = optionalTask.get();

        // Validate task type before casting
        if (!(task instanceof PhoneOutreachTask)) {
            throw new IllegalArgumentException("Task ID " + taskId + " is not a Phone Outreach Task");
        }

        PhoneOutreachTask phoneOutreachTask = (PhoneOutreachTask) task;

        // Transform entity to DTO
        return transformToPhoneOutreachTaskDTO(phoneOutreachTask);
    }

    @Transactional
    public String sentEmailOutreachTask(Long taskId) {
        Optional<Task> optionalTask = taskRepository.findById(taskId);
        if (optionalTask.isEmpty()) return "Task not found.";

        Task task = optionalTask.get();

        if (!(task instanceof EmailOutreachTask)) {
            return "Only EmailOutreachTask can be promoted.";
        }

        // Validate lifecycle type
        if (task.getLifecycle() == null ||
                !LifecycleType.OUTREACH.equalsIgnoreCase(task.getLifecycle().getType())) {
            return "Task not associated with an OUTREACH lifecycle.";
        }


        // Fetch OUTREACH stages from DB (cycleId = 5)
        List<ConstantLifecycle> stages = constantLifecycleRepository.findByCycleId(5L);
        if (stages.isEmpty()) return "No lifecycle stages defined.";

        // Find current stage
        String currentStatus = task.getStatus();
        String nextStatus;
        if (currentStatus.equalsIgnoreCase(LifecycleName.CLOSURE)){
            nextStatus=LifecycleName.COMPLETED;
            Comments comment = new Comments();
            comment.setTask(task);
            comment.setCreatedBy("SYSTEM");
            comment.setCreatedOn(System.currentTimeMillis());
            comment.setIsSystemComment(true);
            comment.setCommentDescription("Task promoted from '" + currentStatus + "' to '" + nextStatus + "'");
            commentsRepository.save(comment);

            // Update task status
            task.setStatus(nextStatus);
            task.setDueDate(System.currentTimeMillis());
            taskRepository.save(task);
            updateMaturityForEmailOutreach(task, currentStatus);

        }else {
            int currentIndex = -1;
            for (int i = 0; i < stages.size(); i++) {
                if (stages.get(i).getCycleName().equalsIgnoreCase(currentStatus)) {
                    currentIndex = i;
                    break;
                }
            }

            if (currentIndex == -1) return "Current status invalid: '" + currentStatus + "'";
            if (currentIndex >= stages.size() - 1) return "Task is already at final stage.";

            // Promote to next stage
            nextStatus = stages.get(currentIndex + 1).getCycleName();

            // Determine days to add
            int daysToAdd = 3; // default
            if (LifecycleName.FOLLOW_UP_3.equalsIgnoreCase(currentStatus) && LifecycleName.CLOSURE.equalsIgnoreCase(nextStatus)) {
                daysToAdd = 4; // add one extra day
            }

            long today = System.currentTimeMillis();
            long baseDate = Math.max(today, task.getDueDate());

            long nextDate =
                    calculateNextWorkingDateMillisForEmailOutreach(baseDate,daysToAdd);

            // Add system comment
            Comments comment = new Comments();
            comment.setTask(task);
            comment.setCreatedBy("SYSTEM");
            comment.setCreatedOn(System.currentTimeMillis());
            comment.setIsSystemComment(true);
            comment.setCommentDescription("Task promoted from '" + currentStatus + "' to '" + nextStatus + "'");
            commentsRepository.save(comment);

            // Update task status
            task.setStatus(nextStatus);
            task.setDueDate(nextDate);
            taskRepository.save(task);
            updateMaturityForEmailOutreach(task, currentStatus);

            if (LifecycleName.MEETING.equalsIgnoreCase(nextStatus)) {
                try {
                    marketingMeetingRegisterService.registerMeeting(task);
                    logger.info("Meeting registered successfully for taskId: {}", taskId);
                } catch (Exception e) {
                    logger.error("Failed to register meeting for taskId: {}", taskId, e);
                }
            }
        }

        return "Promoted to '" + nextStatus + "'";
    }

    private static Long calculateNextWorkingDateMillisForEmailOutreach(Long taskDueDate,int daysToAdd) {

        LocalDate date = Instant.ofEpochMilli(taskDueDate)
                .atZone(ZoneOffset.UTC)
                .toLocalDate();

        int addedDays = 0;

        while (addedDays < daysToAdd) {
            date = date.plusDays(1);
            DayOfWeek day = date.getDayOfWeek();
            if (day != DayOfWeek.FRIDAY && day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                addedDays++;
            }
        }

        return date.atTime(1, 0, 0) // 1:00:00
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
    }

    @Transactional
    public String sentLinkedInOutreachTask(Long taskId) {
        Optional<Task> optionalTask = taskRepository.findById(taskId);
        if (optionalTask.isEmpty()) return "Task not found.";

        Task task = optionalTask.get();

        if (!(task instanceof LinkedInOutreachTask)) {
            return "Only LinkedInOutreachTask can be promoted.";
        }

        // Validate lifecycle type
        if (task.getLifecycle() == null ||
                !LifecycleType.OUTREACH.equalsIgnoreCase(task.getLifecycle().getType())) {
            return "Task not associated with an OUTREACH lifecycle.";
        }


        // Fetch OUTREACH stages from DB (cycleId = 5)
        List<ConstantLifecycle> stages = constantLifecycleRepository.findByCycleId(10L);
        if (stages.isEmpty()) return "No lifecycle stages defined.";

        // Find current stage
        String currentStatus = task.getStatus();
        String nextStatus;
        if (currentStatus.equalsIgnoreCase(LifecycleName.CLOSURE)){
            nextStatus=LifecycleName.COMPLETED;
            Comments comment = new Comments();
            comment.setTask(task);
            comment.setCreatedBy("SYSTEM");
            comment.setCreatedOn(System.currentTimeMillis());
            comment.setIsSystemComment(true);
            comment.setCommentDescription("Task promoted from '" + currentStatus + "' to '" + nextStatus + "'");
            commentsRepository.save(comment);

            // Update task status
            task.setStatus(nextStatus);
            task.setDueDate(System.currentTimeMillis());
            taskRepository.save(task);
            updateMaturityForLinkedInOutreach(task, currentStatus);


        }else {
            int currentIndex = -1;
            for (int i = 0; i < stages.size(); i++) {
                if (stages.get(i).getCycleName().equalsIgnoreCase(currentStatus)) {
                    currentIndex = i;
                    break;
                }
            }

            if (currentIndex == -1) return "Current status invalid: '" + currentStatus + "'";
            if (currentIndex >= stages.size() - 1) return "Task is already at final stage.";

            // Promote to next stage
            nextStatus = stages.get(currentIndex + 1).getCycleName();

            Long nextDueDateMillis;
            Long nextDate;


            // Default delay (in working days)
            int daysToAdd = 0;

            // Define specific gaps based on current → next status
            if (LifecycleName.WAITING_FOR_ACCEPTANCE.equalsIgnoreCase(currentStatus)
                    && LifecycleName.INTRO.equalsIgnoreCase(nextStatus)) {
                // same-day — no delay
                daysToAdd = 0;
            } else if (LifecycleName.INTRO.equalsIgnoreCase(currentStatus)
                    && LifecycleName.FOLLOW_UP_1.equalsIgnoreCase(nextStatus)) {
                daysToAdd = 3;
            } else if (LifecycleName.FOLLOW_UP_1.equalsIgnoreCase(currentStatus)
                    && LifecycleName.FOLLOW_UP_2.equalsIgnoreCase(nextStatus)) {
                daysToAdd = 2;
            } else if (LifecycleName.FOLLOW_UP_2.equalsIgnoreCase(currentStatus)
                    && LifecycleName.FOLLOW_UP_3.equalsIgnoreCase(nextStatus)) {
                daysToAdd = 3;
            } else if (LifecycleName.FOLLOW_UP_3.equalsIgnoreCase(currentStatus)
                    && LifecycleName.CLOSURE.equalsIgnoreCase(nextStatus)) {
                daysToAdd = 5;
            }
            long today = System.currentTimeMillis();
            long baseDate = Math.max(today, task.getDueDate());

            // Compute due date
            if (daysToAdd == 0) {
                // same-day (1 AM)
                nextDate = Instant.ofEpochMilli(baseDate)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                        .atTime(1, 0, 0)
                        .atZone(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli();
            } else {
                nextDate =
                        calculateNextWorkingDateMillisForEmailOutreach(baseDate, daysToAdd);

            }


            // Add system comment
            Comments comment = new Comments();
            comment.setTask(task);
            comment.setCreatedBy("SYSTEM");
            comment.setCreatedOn(System.currentTimeMillis());
            comment.setIsSystemComment(true);
            comment.setCommentDescription("Task promoted from '" + currentStatus + "' to '" + nextStatus + "'");
            commentsRepository.save(comment);

            // Update task status
            task.setStatus(nextStatus);
            task.setDueDate(nextDate);
            taskRepository.save(task);
            updateMaturityForLinkedInOutreach(task, currentStatus);

            if (LifecycleName.WAITING_FOR_ACCEPTANCE.equalsIgnoreCase(currentStatus)
                    && LifecycleName.INTRO.equalsIgnoreCase(nextStatus)) {

                updateMaturityForLinkedInOutreach(task, LifecycleName.ACCEPTED);
            }

            if (LifecycleName.MEETING.equalsIgnoreCase(nextStatus)) {
                try {
                    marketingMeetingRegisterService.registerMeeting(task);
                    logger.info("Meeting registered successfully for taskId: {}", taskId);
                } catch (Exception e) {
                    logger.error("Failed to register meeting for taskId: {}", taskId, e);
                }
            }
        }

        return "Promoted to '" + nextStatus + "'";
    }

    @Transactional
    public String notActiveOnLinkedIn(Long taskId) {
        Optional<Task> optionalTask = taskRepository.findById(taskId);
        if (optionalTask.isEmpty()) return "Task not found.";

        Task task = optionalTask.get();

        if (!(task instanceof LinkedInOutreachTask)) {
            return "Only LinkedInOutreachTask can be promoted.";
        }

        // Validate lifecycle type
        if (task.getLifecycle() == null ||
                !LifecycleType.OUTREACH.equalsIgnoreCase(task.getLifecycle().getType())) {
            return "Task not associated with an OUTREACH lifecycle.";
        }

        Comments comment = new Comments();
        comment.setTask(task);
        comment.setCreatedBy("SYSTEM");
        comment.setCreatedOn(System.currentTimeMillis());
        comment.setIsSystemComment(true);
        comment.setCommentDescription("User is not active on LinkedIn");
        commentsRepository.save(comment);

        task.setStatus(LifecycleName.COMPLETED);

        return "Promoted to Completed";
    }


    private static Long calculateNextWorkingDateMillisForLinkedInOutreach(Long taskDueDate, int daysToAdd) {

        LocalDate date = Instant.ofEpochMilli(taskDueDate)
                .atZone(ZoneOffset.UTC)
                .toLocalDate();

        int addedDays = 0;

        while (addedDays < daysToAdd) {
            date = date.plusDays(1);
            DayOfWeek day = date.getDayOfWeek();
            if (day != DayOfWeek.FRIDAY &&day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                addedDays++;
            }
        }

        return date.atTime(1, 0, 0) // 1:00:00
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
    }

    private void updateMaturityForLinkedInOutreach(Task task, String currentStatus) {
        Long campaignId = null;

        if (task.getLifecycle().getCampaign() != null) {
            campaignId = task.getLifecycle().getCampaign().getCampaignId();
        } else if (task.getLifecycle().getLead() != null &&
                task.getLifecycle().getLead().getCampaign() != null) {
            campaignId = task.getLifecycle().getLead().getCampaign().getCampaignId();
        }

        if (campaignId != null) {
            linkedInOutreachCampaignMaturityRecordRegisterService
                    .updateMaturityReport(campaignId, currentStatus);
        }
    }

    private void updateMaturityForEmailOutreach(Task task, String currentStatus) {
        Long campaignId = null;

        if (task.getLifecycle().getCampaign() != null) {
            campaignId = task.getLifecycle().getCampaign().getCampaignId();
        } else if (task.getLifecycle().getLead() != null &&
                task.getLifecycle().getLead().getCampaign() != null) {
            campaignId = task.getLifecycle().getLead().getCampaign().getCampaignId();
        }

        if (campaignId != null) {
            maturityService.updateMaturityReport(campaignId, currentStatus);
        }
    }

    public List<LinkedInOutreachTaskDTO> getLinkedInOutreachWaitingAcceptanceTask(String username) {

        String targetStatus = LifecycleName.WAITING_FOR_ACCEPTANCE;

        return taskRepository.findByStatusIgnoreCase(targetStatus).stream()
                // Filter only LinkedInOutreachTask instances
                .filter(task -> task instanceof LinkedInOutreachTask)
                .map(task -> (LinkedInOutreachTask) task)
                // Belongs to a Lead
                .filter(task -> {
                    Lifecycle lifecycle = task.getLifecycle();
                    return lifecycle != null && lifecycle.getLead() != null;
                })
                // Filter by assigned user (if applicable)
                .filter(task -> username == null || username.equalsIgnoreCase(task.getAssignTo()))
                // Map to DTO
                .map(this::transformToLinkedInOutreachTaskDTO)
                .collect(Collectors.toList());
    }


    private Task transformToTask(TaskDTO taskDTO) {
        // Determine if this is an outreach task
        boolean isOutreach = taskDTO instanceof EmailOutreachTaskDTO;
        Task task;

        // Handle existing task (update case)
        if (taskDTO.getTaskId() != null) {
            task = taskRepository.findById(taskDTO.getTaskId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Task not found with ID: " + taskDTO.getTaskId()));

            // Validate task type consistency
            if (isOutreach && !(task instanceof EmailOutreachTask)) {
                throw new IllegalStateException(
                        "Existing task is not an OutreachTask. ID: " + taskDTO.getTaskId());
            }
            if (!isOutreach && task instanceof EmailOutreachTask) {
                throw new IllegalStateException(
                        "Existing task is an OutreachTask. ID: " + taskDTO.getTaskId());
            }
        }
        // Handle new task (create case)
        else {
            task = isOutreach ? new EmailOutreachTask() : new Task();
            task.setCreatedOn(System.currentTimeMillis());
        }

        // Copy all common fields from TaskDTO
        copyCommonTaskFields(task, taskDTO);

        // Handle outreach-specific fields if applicable
        if (isOutreach && task instanceof EmailOutreachTask) {
            handleOutreachTaskFields((EmailOutreachTask) task, (EmailOutreachTaskDTO) taskDTO);
        }

        // Handle parent task assignment
        handleParentTaskAssignment(task, taskDTO);
        // Handle lifecycle assignment
        handleLifecycleAssignment(task, taskDTO, isOutreach);

        return task;
    }

    private void copyCommonTaskFields(Task task, TaskDTO taskDTO) {
        task.setTaskName(taskDTO.getTaskName());
        task.setDescription(taskDTO.getDescription());
        task.setAssignTo(taskDTO.getAssignTo());
        task.setSendToAssignee(taskDTO.isSendToAssignee());
        task.setDuration(taskDTO.getDuration());
        task.setDurationValue(taskDTO.getDurationValue());
        task.setDueDate(taskDTO.getDueDate());
        task.setWeeklyTaskSequence(taskDTO.getWeeklyTaskSequence());
        task.setIsSystemComment(taskDTO.getIsSystemComment());
        if(taskDTO.getCreatedOn() == null){
            task.setCreatedOn(System.currentTimeMillis());
        }
        // Set status from DTO if provided
        if (taskDTO.getStatus() != null) {
            task.setStatus(taskDTO.getStatus());
        }
    }

    private void handleOutreachTaskFields(EmailOutreachTask emailOutreachTask, EmailOutreachTaskDTO emailOutreachTaskDTO) {
        Contact contact;

        // For existing contact (update case)
        if (emailOutreachTaskDTO.getContactId() != null) {
            contact = contactRepository.findById(emailOutreachTaskDTO.getContactId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Contact not found with ID: " + emailOutreachTaskDTO.getContactId()));

            // Update contact details from DTO
            contact.setFirstName(emailOutreachTaskDTO.getContactFirstName());
            contact.setLastName(emailOutreachTaskDTO.getContactLastName());
            contact.setEmailID(emailOutreachTaskDTO.getContactEmailID());
            contact.setPhoneNo(emailOutreachTaskDTO.getContactPhoneNo());
            contact.setCity(emailOutreachTaskDTO.getContactCity());
            contact.setState(emailOutreachTaskDTO.getContactState());
            contact.setCountry(emailOutreachTaskDTO.getContactCountry());

            // handle Company as entity, not String
            if (emailOutreachTaskDTO.getContactCompany() != null) {
                Company company = companyRepository.findByNameIgnoreCase(emailOutreachTaskDTO.getContactCompany())
                        .orElseGet(() -> {
                            Company newCompany = new Company();
                            newCompany.setName(emailOutreachTaskDTO.getContactCompany());
                            return companyRepository.save(newCompany);
                        });
                contact.setCompany(company);
            }
            contactRepository.save(contact);
        }
        // For new contact (create case)
        else {
            contact = new Contact();
            contact.setFirstName(emailOutreachTaskDTO.getContactFirstName());
            contact.setLastName(emailOutreachTaskDTO.getContactLastName());
            contact.setEmailID(emailOutreachTaskDTO.getContactEmailID());
            contact.setPhoneNo(emailOutreachTaskDTO.getContactPhoneNo());
            contact.setCity(emailOutreachTaskDTO.getContactCity());
            contact.setState(emailOutreachTaskDTO.getContactState());
            contact.setCountry(emailOutreachTaskDTO.getContactCountry());

            // FIX: attach company
            if (emailOutreachTaskDTO.getContactCompany() != null) {
                Company company = companyRepository.findByNameIgnoreCase(emailOutreachTaskDTO.getContactCompany())
                        .orElseGet(() -> {
                            Company newCompany = new Company();
                            newCompany.setName(emailOutreachTaskDTO.getContactCompany());
                            return companyRepository.save(newCompany);
                        });
                contact.setCompany(company);
            }

            contact = contactRepository.save(contact);
        }

        emailOutreachTask.setRelatedContact(contact);
    }

    private void handleLifecycleAssignment(Task task, TaskDTO taskDTO, boolean isOutreach) {
        // Skip lifecycle assignment if this is a subtask (has parent)
        if (task.getParentTask() != null) {
            return;
        }
        // For regular tasks (non-parent tasks)
        if (taskDTO.getLifecycleId() != null) {
            Lifecycle lifecycle = lifecycleRepository.findById(taskDTO.getLifecycleId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Lifecycle not found with ID: " + taskDTO.getLifecycleId()));

            if (isOutreach && !LifecycleType.OUTREACH.equalsIgnoreCase(lifecycle.getType())) {
                throw new IllegalArgumentException(
                        "OutreachTask requires an OUTREACH lifecycle. ID: " + taskDTO.getLifecycleId());
            }
            if (!isOutreach && LifecycleType.OUTREACH.equalsIgnoreCase(lifecycle.getType())) {
                throw new IllegalArgumentException(
                        "Regular Task cannot use OUTREACH lifecycle. ID: " + taskDTO.getLifecycleId());
            }

            task.setLifecycle(lifecycle);
        }
        else if (taskDTO.getTaskId() == null) {
            // Only set default lifecycle for new tasks
            Long defaultCycleId = isOutreach ? 5L : 6L;
            task.setLifecycle(lifecycleRepository.findById(defaultCycleId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Default lifecycle not found for type: " + (isOutreach ? "OUTREACH" : "DEFAULT"))));
        }
    }

    private void handleParentTaskAssignment(Task task, TaskDTO taskDTO) {
        if (taskDTO.getParentTaskId() != null) {
            Task parentTask = taskRepository.findById(taskDTO.getParentTaskId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Parent task not found with ID: " + taskDTO.getParentTaskId()));

            // Check for self-reference
            if (task.getTaskId() != null && task.getTaskId().equals(parentTask.getTaskId())) {
                throw new IllegalArgumentException("Task cannot be its own parent");
            }

            // Set parent and nullify lifecycle (parent tasks shouldn't have lifecycles)
            task.setParentTask(parentTask);
            task.setLifecycle(null);
        } else {
            task.setParentTask(null);
        }
    }


    public EmailOutreachTask transformToEmailOutreachTask(EmailOutreachTaskDTO emailOutreachTaskDTO) {
        // Create new OutreachTask or get existing one
        EmailOutreachTask emailOutreachTask = (emailOutreachTaskDTO.getTaskId() != null) ?
                (EmailOutreachTask) taskRepository.findById(emailOutreachTaskDTO.getTaskId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "OutreachTask not found with ID: " + emailOutreachTaskDTO.getTaskId())) :
                new EmailOutreachTask();

        // Set common task fields only if they are not null in DTO
        if (emailOutreachTaskDTO.getTaskName() != null) {
            emailOutreachTask.setTaskName(emailOutreachTaskDTO.getTaskName());
        }
        if (emailOutreachTaskDTO.getDescription() != null) {
            emailOutreachTask.setDescription(emailOutreachTaskDTO.getDescription());
        }
        if (emailOutreachTaskDTO.getAssignTo() != null) {
            emailOutreachTask.setAssignTo(emailOutreachTaskDTO.getAssignTo());
        }
        if (emailOutreachTaskDTO.getDuration() != null) {
            emailOutreachTask.setDuration(emailOutreachTaskDTO.getDuration());
        }
        if (emailOutreachTaskDTO.getDurationValue() != null) {
            emailOutreachTask.setDurationValue(emailOutreachTaskDTO.getDurationValue());
        }
        if (emailOutreachTaskDTO.getDueDate() != null) {
            emailOutreachTask.setDueDate(emailOutreachTaskDTO.getDueDate());
        }
        if (emailOutreachTaskDTO.getStatus() != null) {
            emailOutreachTask.setStatus(emailOutreachTaskDTO.getStatus());
        }
        if (emailOutreachTaskDTO.getIsSystemComment() != null) {
            emailOutreachTask.setIsSystemComment(emailOutreachTaskDTO.getIsSystemComment());
        }

        // Set createdOn for new tasks
        if (emailOutreachTaskDTO.getTaskId() == null) {
            emailOutreachTask.setCreatedOn(System.currentTimeMillis());
        }

        // Handle contact information
        if (emailOutreachTaskDTO.getContactId() != null) {
            Contact contact = contactRepository.findById(emailOutreachTaskDTO.getContactId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Contact not found with ID: " + emailOutreachTaskDTO.getContactId()));

            // Update contact fields from DTO only if they are not null
            if (emailOutreachTaskDTO.getContactFirstName() != null) {
                contact.setFirstName(emailOutreachTaskDTO.getContactFirstName());
            }
            if (emailOutreachTaskDTO.getContactLastName() != null) {
                contact.setLastName(emailOutreachTaskDTO.getContactLastName());
            }
            if (emailOutreachTaskDTO.getContactEmailID() != null) {
                contact.setEmailID(emailOutreachTaskDTO.getContactEmailID());
            }
            if (emailOutreachTaskDTO.getContactPhoneNo() != null) {
                contact.setPhoneNo(emailOutreachTaskDTO.getContactPhoneNo());
            }

            // COMPANY UPDATE — BY ID ONLY
            if (emailOutreachTaskDTO.getContactCompanyId() != null) {

                Company company = companyRepository
                        .findById(emailOutreachTaskDTO.getContactCompanyId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Company not found with id: " +
                                                emailOutreachTaskDTO.getContactCompanyId()
                                )
                        );

                String newName = emailOutreachTaskDTO.getContactCompany();

                // Update company name ONLY if explicitly provided
                if (emailOutreachTaskDTO.getContactCompany() != null &&
                        !emailOutreachTaskDTO.getContactCompany().equalsIgnoreCase(company.getName())) {

                    company.setName(emailOutreachTaskDTO.getContactCompany());
                    companyRepository.save(company); // persists rename

                    //SYNC LEAD NAME USING LIFECYCLE
                    Lifecycle lifecycle = emailOutreachTask.getLifecycle();
                    if (lifecycle != null && lifecycle.getLead() != null) {
                        Lead lead = lifecycle.getLead();
                        lead.setLeadName(newName);
                        lead.setLeadTitle(newName);
                    }
                }

                contact.setCompany(company);
            }



            if (emailOutreachTaskDTO.getContactCity() != null) {
                contact.setCity(emailOutreachTaskDTO.getContactCity());
            }
            if (emailOutreachTaskDTO.getContactState() != null) {
                contact.setState(emailOutreachTaskDTO.getContactState());
            }
            if (emailOutreachTaskDTO.getContactCountry() != null) {
                contact.setCountry(emailOutreachTaskDTO.getContactCountry());
            }
            if (emailOutreachTaskDTO.getLinkedInUrl() != null) {
                contact.setLinkedInUrl(emailOutreachTaskDTO.getLinkedInUrl());
            }
            if(emailOutreachTaskDTO.getDesignation() != null){
                contact.setDesignation(emailOutreachTaskDTO.getDesignation());
            }

            contactRepository.save(contact);
            emailOutreachTask.setRelatedContact(contact);
        }
        else if (emailOutreachTaskDTO.getTaskId() == null) {
            throw new IllegalArgumentException("Contact ID is required for new outreach tasks");
        }

        // Handle lifecycle assignment
        if (emailOutreachTaskDTO.getLifecycleId() != null) {
            Lifecycle lifecycle = lifecycleRepository.findById(emailOutreachTaskDTO.getLifecycleId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Lifecycle not found with ID: " + emailOutreachTaskDTO.getLifecycleId()));

            if (!LifecycleType.OUTREACH.equalsIgnoreCase(lifecycle.getType())) {
                throw new IllegalArgumentException(
                        "OutreachTask requires a lifecycle of type '" + TaskType.EMAIL_OUTREACH_TASK +
                                "'. Found: " + lifecycle.getType());
            }

            emailOutreachTask.setLifecycle(lifecycle);
        } else if (emailOutreachTaskDTO.getTaskId() == null) {
            // Assign default OUTREACH lifecycle for new outreach tasks
            Lifecycle defaultOutreachLifecycle = lifecycleRepository.findByType(TaskType.EMAIL_OUTREACH_TASK)
                    .orElseGet(() -> lifecycleRepository.findById(5L)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Default " + TaskType.EMAIL_OUTREACH_TASK + " lifecycle not found")));

            emailOutreachTask.setLifecycle(defaultOutreachLifecycle);
        }

        // Handle parent task assignment
        if (emailOutreachTaskDTO.getParentTaskId() != null) {
            Task parentTask = taskRepository.findById(emailOutreachTaskDTO.getParentTaskId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Parent task not found with ID: " + emailOutreachTaskDTO.getParentTaskId()));
            emailOutreachTask.setParentTask(parentTask);
        }

        return emailOutreachTask;
    }

    public LinkedInOutreachTask transformToLinkedInOutreachTask(LinkedInOutreachTaskDTO linkedInOutreachTaskDTO) {
        // Create new LinkedInOutreachTask or get existing one
        LinkedInOutreachTask linkedInOutreachTask = (linkedInOutreachTaskDTO.getTaskId() != null) ?
                (LinkedInOutreachTask) taskRepository.findById(linkedInOutreachTaskDTO.getTaskId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "LinkedInOutreachTask not found with ID: " + linkedInOutreachTaskDTO.getTaskId())) :
                new LinkedInOutreachTask();

        // Set common task fields if present in DTO
        if (linkedInOutreachTaskDTO.getTaskName() != null) {
            linkedInOutreachTask.setTaskName(linkedInOutreachTaskDTO.getTaskName());
        }
        if (linkedInOutreachTaskDTO.getDescription() != null) {
            linkedInOutreachTask.setDescription(linkedInOutreachTaskDTO.getDescription());
        }
        if (linkedInOutreachTaskDTO.getAssignTo() != null) {
            linkedInOutreachTask.setAssignTo(linkedInOutreachTaskDTO.getAssignTo());
        }
        if (linkedInOutreachTaskDTO.getDuration() != null) {
            linkedInOutreachTask.setDuration(linkedInOutreachTaskDTO.getDuration());
        }
        if (linkedInOutreachTaskDTO.getDurationValue() != null) {
            linkedInOutreachTask.setDurationValue(linkedInOutreachTaskDTO.getDurationValue());
        }
        if (linkedInOutreachTaskDTO.getDueDate() != null) {
            linkedInOutreachTask.setDueDate(linkedInOutreachTaskDTO.getDueDate());
        }
        if (linkedInOutreachTaskDTO.getStatus() != null) {
            linkedInOutreachTask.setStatus(linkedInOutreachTaskDTO.getStatus());
        }
        if (linkedInOutreachTaskDTO.getIsSystemComment() != null) {
            linkedInOutreachTask.setIsSystemComment(linkedInOutreachTaskDTO.getIsSystemComment());
        }

        // Set createdOn for new tasks
        if (linkedInOutreachTaskDTO.getTaskId() == null) {
            linkedInOutreachTask.setCreatedOn(System.currentTimeMillis());
        }

        // Handle contact information
        if (linkedInOutreachTaskDTO.getContactId() != null) {
            Contact contact = contactRepository.findById(linkedInOutreachTaskDTO.getContactId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Contact not found with ID: " + linkedInOutreachTaskDTO.getContactId()));

            if (linkedInOutreachTaskDTO.getContactFirstName() != null) contact.setFirstName(linkedInOutreachTaskDTO.getContactFirstName());
            if (linkedInOutreachTaskDTO.getContactLastName() != null) contact.setLastName(linkedInOutreachTaskDTO.getContactLastName());
            if (linkedInOutreachTaskDTO.getContactEmailID() != null) contact.setEmailID(linkedInOutreachTaskDTO.getContactEmailID());
            if (linkedInOutreachTaskDTO.getContactPhoneNo() != null) contact.setPhoneNo(linkedInOutreachTaskDTO.getContactPhoneNo());

//            if (linkedInOutreachTaskDTO.getContactCompany() != null) {
//                final String companyName = linkedInOutreachTaskDTO.getContactCompany(); // final for lambda
//                Company company = companyRepository.findByNameIgnoreCase(companyName)
//                        .orElseGet(() -> {
//                            Company newCompany = new Company();
//                            newCompany.setName(companyName);
//                            return companyRepository.save(newCompany);
//                        });
//                contact.setCompany(company);
//            }

            // COMPANY UPDATE — BY ID ONLY
            if (linkedInOutreachTaskDTO.getContactCompanyId() != null) {

                Company company = companyRepository
                        .findById(linkedInOutreachTaskDTO.getContactCompanyId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Company not found with id: " +
                                                linkedInOutreachTaskDTO.getContactCompanyId()
                                )
                        );

                String newName = linkedInOutreachTaskDTO.getContactCompany();

                // Update company name ONLY if explicitly provided
                if (linkedInOutreachTaskDTO.getContactCompany() != null &&
                        !linkedInOutreachTaskDTO.getContactCompany().equalsIgnoreCase(company.getName())) {

                    company.setName(linkedInOutreachTaskDTO.getContactCompany());
                    companyRepository.save(company); // persists rename

                    //SYNC LEAD NAME USING LIFECYCLE
                    Lifecycle lifecycle = linkedInOutreachTask.getLifecycle();
                    if (lifecycle != null && lifecycle.getLead() != null) {
                        Lead lead = lifecycle.getLead();
                        lead.setLeadName(newName);
                        lead.setLeadTitle(newName);
                    }
                }

                contact.setCompany(company);
            }

            if (linkedInOutreachTaskDTO.getContactCity() != null) contact.setCity(linkedInOutreachTaskDTO.getContactCity());
            if (linkedInOutreachTaskDTO.getContactState() != null) contact.setState(linkedInOutreachTaskDTO.getContactState());
            if (linkedInOutreachTaskDTO.getContactCountry() != null) contact.setCountry(linkedInOutreachTaskDTO.getContactCountry());
            if (linkedInOutreachTaskDTO.getLinkedInUrl() != null) contact.setLinkedInUrl(linkedInOutreachTaskDTO.getLinkedInUrl());
            if (linkedInOutreachTaskDTO.getDesignation() != null) contact.setDesignation(linkedInOutreachTaskDTO.getDesignation());

            contactRepository.save(contact);
            linkedInOutreachTask.setRelatedContact(contact);
        } else if (linkedInOutreachTaskDTO.getTaskId() == null) {
            throw new IllegalArgumentException("Contact ID is required for new LinkedIn outreach tasks");
        }

        // Handle lifecycle assignment
        if (linkedInOutreachTaskDTO.getLifecycleId() != null) {
            Lifecycle lifecycle = lifecycleRepository.findById(linkedInOutreachTaskDTO.getLifecycleId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Lifecycle not found with ID: " + linkedInOutreachTaskDTO.getLifecycleId()));

            if (!LifecycleType.OUTREACH.equalsIgnoreCase(lifecycle.getType())) {
                throw new IllegalArgumentException(
                        "LinkedInOutreachTask requires a lifecycle of type '" + TaskType.LINKEDIN_OUTREACH_TASK +
                                "'. Found: " + lifecycle.getType());
            }

            linkedInOutreachTask.setLifecycle(lifecycle);
        } else if (linkedInOutreachTaskDTO.getTaskId() == null) {
            // Assign default LinkedIn OUTREACH lifecycle for new tasks
            Lifecycle defaultLinkedInLifecycle = lifecycleRepository.findByType(TaskType.LINKEDIN_OUTREACH_TASK)
                    .orElseGet(() -> lifecycleRepository.findById(5L)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Default " + TaskType.LINKEDIN_OUTREACH_TASK + " lifecycle not found")));

            linkedInOutreachTask.setLifecycle(defaultLinkedInLifecycle);
        }

        // Handle parent task assignment
        if (linkedInOutreachTaskDTO.getParentTaskId() != null) {
            Task parentTask = taskRepository.findById(linkedInOutreachTaskDTO.getParentTaskId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Parent task not found with ID: " + linkedInOutreachTaskDTO.getParentTaskId()));
            linkedInOutreachTask.setParentTask(parentTask);
        }

        return linkedInOutreachTask;
    }

    public PhoneOutreachTask transformToPhoneOutreachTask(PhoneOutreachTaskDTO phoneOutreachTaskDTO) {
        PhoneOutreachTask phoneOutreachTask = (phoneOutreachTaskDTO.getTaskId() != null)
                ? (PhoneOutreachTask) taskRepository.findById(phoneOutreachTaskDTO.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "PhoneOutreachTask not found with ID: " + phoneOutreachTaskDTO.getTaskId()))
                : new PhoneOutreachTask();

        // Basic fields
        if (phoneOutreachTaskDTO.getTaskName() != null)
            phoneOutreachTask.setTaskName(phoneOutreachTaskDTO.getTaskName());
        if (phoneOutreachTaskDTO.getDescription() != null)
            phoneOutreachTask.setDescription(phoneOutreachTaskDTO.getDescription());
        if (phoneOutreachTaskDTO.getAssignTo() != null)
            phoneOutreachTask.setAssignTo(phoneOutreachTaskDTO.getAssignTo());
        if (phoneOutreachTaskDTO.getDuration() != null)
            phoneOutreachTask.setDuration(phoneOutreachTaskDTO.getDuration());
        if (phoneOutreachTaskDTO.getDurationValue() != null)
            phoneOutreachTask.setDurationValue(phoneOutreachTaskDTO.getDurationValue());
        if (phoneOutreachTaskDTO.getDueDate() != null)
            phoneOutreachTask.setDueDate(phoneOutreachTaskDTO.getDueDate());
        if (phoneOutreachTaskDTO.getStatus() != null)
            phoneOutreachTask.setStatus(phoneOutreachTaskDTO.getStatus());
        if (phoneOutreachTaskDTO.getIsSystemComment() != null)
            phoneOutreachTask.setIsSystemComment(phoneOutreachTaskDTO.getIsSystemComment());

        if (phoneOutreachTaskDTO.getTaskId() == null)
            phoneOutreachTask.setCreatedOn(System.currentTimeMillis());

        if (phoneOutreachTaskDTO.getDidNotAnswer() != null)
            phoneOutreachTask.setDidNotAnswer(phoneOutreachTaskDTO.getDidNotAnswer());
        if (phoneOutreachTaskDTO.getNotReachable() != null)
            phoneOutreachTask.setNotReachable(phoneOutreachTaskDTO.getNotReachable());


        // Handle contact
        if (phoneOutreachTaskDTO.getContactId() != null) {
            Contact contact = contactRepository.findById(phoneOutreachTaskDTO.getContactId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Contact not found with ID: " + phoneOutreachTaskDTO.getContactId()));

            if (phoneOutreachTaskDTO.getContactFirstName() != null)
                contact.setFirstName(phoneOutreachTaskDTO.getContactFirstName());
            if (phoneOutreachTaskDTO.getContactLastName() != null)
                contact.setLastName(phoneOutreachTaskDTO.getContactLastName());
            if (phoneOutreachTaskDTO.getContactEmailID() != null)
                contact.setEmailID(phoneOutreachTaskDTO.getContactEmailID());
            if (phoneOutreachTaskDTO.getContactPhoneNo() != null)
                contact.setPhoneNo(phoneOutreachTaskDTO.getContactPhoneNo());

            // COMPANY UPDATE — BY ID ONLY
            if (phoneOutreachTaskDTO.getContactCompanyId() != null) {

                Company company = companyRepository
                        .findById(phoneOutreachTaskDTO.getContactCompanyId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Company not found with id: " +
                                                phoneOutreachTaskDTO.getContactCompanyId()
                                )
                        );

                String newName = phoneOutreachTaskDTO.getContactCompany();

                // Update company name ONLY if explicitly provided
                if (phoneOutreachTaskDTO.getContactCompany() != null &&
                        !phoneOutreachTaskDTO.getContactCompany().equalsIgnoreCase(company.getName())) {

                    company.setName(phoneOutreachTaskDTO.getContactCompany());
                    companyRepository.save(company); // persists rename

                    //SYNC LEAD NAME USING LIFECYCLE
                    Lifecycle lifecycle = phoneOutreachTask.getLifecycle();
                    if (lifecycle != null && lifecycle.getLead() != null) {
                        Lead lead = lifecycle.getLead();
                        lead.setLeadName(newName);
                        lead.setLeadTitle(newName);
                    }

                }

                contact.setCompany(company);
            }

            if (phoneOutreachTaskDTO.getContactCity() != null)
                contact.setCity(phoneOutreachTaskDTO.getContactCity());
            if (phoneOutreachTaskDTO.getContactState() != null)
                contact.setState(phoneOutreachTaskDTO.getContactState());
            if (phoneOutreachTaskDTO.getContactCountry() != null)
                contact.setCountry(phoneOutreachTaskDTO.getContactCountry());
            if (phoneOutreachTaskDTO.getLinkedInUrl() != null)
                contact.setLinkedInUrl(phoneOutreachTaskDTO.getLinkedInUrl());
            if (phoneOutreachTaskDTO.getDesignation() != null)
                contact.setDesignation(phoneOutreachTaskDTO.getDesignation());

            contactRepository.save(contact);
            phoneOutreachTask.setRelatedContact(contact);
        } else if (phoneOutreachTaskDTO.getTaskId() == null) {
            throw new IllegalArgumentException("Contact ID is required for new phone outreach tasks");
        }

        // Handle lifecycle
        if (phoneOutreachTaskDTO.getLifecycleId() != null) {
            Lifecycle lifecycle = lifecycleRepository.findById(phoneOutreachTaskDTO.getLifecycleId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Lifecycle not found with ID: " + phoneOutreachTaskDTO.getLifecycleId()));

            if (!LifecycleType.OUTREACH.equalsIgnoreCase(lifecycle.getType())) {
                throw new IllegalArgumentException(
                        "PhoneOutreachTask requires a lifecycle of type 'OUTREACH'. Found: " + lifecycle.getType());
            }

            phoneOutreachTask.setLifecycle(lifecycle);
        } else if (phoneOutreachTaskDTO.getTaskId() == null) {
            Lifecycle defaultOutreachLifecycle = lifecycleRepository.findByType(TaskType.PHONE_OUTREACH_TASK)
                    .orElseGet(() -> lifecycleRepository.findById(5L)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Default " + TaskType.PHONE_OUTREACH_TASK + " lifecycle not found")));
            phoneOutreachTask.setLifecycle(defaultOutreachLifecycle);
        }

        // Handle parent task
        if (phoneOutreachTaskDTO.getParentTaskId() != null) {
            Task parentTask = taskRepository.findById(phoneOutreachTaskDTO.getParentTaskId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Parent task not found with ID: " + phoneOutreachTaskDTO.getParentTaskId()));
            phoneOutreachTask.setParentTask(parentTask);
        }

        return phoneOutreachTask;
    }

    public Contact transformToContact(TaskDTO taskDTO) {
        if (taskDTO == null) return null;


        Long companyId = null;

        // Determine if taskDTO has contact fields
        String firstName = null, lastName = null, email = null, phone = null, companyName = null;
        String city = null, state = null, country = null, linkedInUrl = null, designation = null;
        Long contactId = null;

        if (taskDTO instanceof EmailOutreachTaskDTO emailDTO) {
            firstName = emailDTO.getContactFirstName();
            lastName = emailDTO.getContactLastName();
            email = emailDTO.getContactEmailID();
            phone = emailDTO.getContactPhoneNo();
            companyName = emailDTO.getContactCompany();
            city = emailDTO.getContactCity();
            state = emailDTO.getContactState();
            country = emailDTO.getContactCountry();
            linkedInUrl = emailDTO.getLinkedInUrl();
            designation = emailDTO.getDesignation();
            contactId = emailDTO.getContactId();
            companyId = emailDTO.getContactCompanyId();
        } else if (taskDTO instanceof LinkedInOutreachTaskDTO linkedInDTO) {
            firstName = linkedInDTO.getContactFirstName();
            lastName = linkedInDTO.getContactLastName();
            email = linkedInDTO.getContactEmailID();
            phone = linkedInDTO.getContactPhoneNo();
            companyName = linkedInDTO.getContactCompany();
            city = linkedInDTO.getContactCity();
            state = linkedInDTO.getContactState();
            country = linkedInDTO.getContactCountry();
            linkedInUrl = linkedInDTO.getLinkedInUrl();
            designation = linkedInDTO.getDesignation();
            contactId = linkedInDTO.getContactId();
            companyId = linkedInDTO.getContactCompanyId();
        }
        // Add more outreach types here if needed
        else if (taskDTO instanceof PhoneOutreachTaskDTO phoneDTO) {
            firstName = phoneDTO.getContactFirstName();
            lastName = phoneDTO.getContactLastName();
            email = phoneDTO.getContactEmailID();
            phone = phoneDTO.getContactPhoneNo();
            companyName = phoneDTO.getContactCompany();
            city = phoneDTO.getContactCity();
            state = phoneDTO.getContactState();
            country = phoneDTO.getContactCountry();
            linkedInUrl = phoneDTO.getLinkedInUrl();
            designation = phoneDTO.getDesignation();
            contactId = phoneDTO.getContactId();
            companyId = phoneDTO.getContactCompanyId();
        }

        // Fetch existing contact or create new
        Contact contact = (contactId != null)
                ? contactRepository.findById(contactId).orElse(new Contact())
                : new Contact();

        //COMPANY UPDATE BY ID ONLY
        if (companyId != null) {
            final Long finalCompanyId = companyId;
            Company company = companyRepository.findById(finalCompanyId)
                    .orElseThrow(() ->
                            new IllegalArgumentException("Company not found with id: " + finalCompanyId)
                    );
            contact.setCompany(company);
        }

        if (firstName != null) contact.setFirstName(firstName);
        if (lastName != null) contact.setLastName(lastName);
        if (email != null) contact.setEmailID(email);
        if (phone != null) contact.setPhoneNo(phone);
        if (city != null) contact.setCity(city);
        if (state != null) contact.setState(state);
        if (country != null) contact.setCountry(country);
        if (linkedInUrl != null) contact.setLinkedInUrl(linkedInUrl);
        if (designation != null) contact.setDesignation(designation);

        return contact;
    }

    public EmailOutreachTaskDTO transformToEmailOutreachTaskDTO(EmailOutreachTask emailOutreachTask)
    {
        EmailOutreachTaskDTO emailOutreachTaskDTO = new EmailOutreachTaskDTO();

        emailOutreachTaskDTO.setTaskId(emailOutreachTask.getTaskId());
        emailOutreachTaskDTO.setTaskName(emailOutreachTask.getTaskName());
        emailOutreachTaskDTO.setDescription(emailOutreachTask.getDescription());
        emailOutreachTaskDTO.setAssignTo(emailOutreachTask.getAssignTo());
        emailOutreachTaskDTO.setSendToAssignee(emailOutreachTask.isSendToAssignee());
        emailOutreachTaskDTO.setDuration(emailOutreachTask.getDuration());
        emailOutreachTaskDTO.setDurationValue(emailOutreachTask.getDurationValue());
        emailOutreachTaskDTO.setDueDate(emailOutreachTask.getDueDate());
        emailOutreachTaskDTO.setCreatedOn(emailOutreachTask.getCreatedOn());
        emailOutreachTaskDTO.setIsSystemComment(emailOutreachTask.getIsSystemComment());
        emailOutreachTaskDTO.setStatus(emailOutreachTask.getStatus());
        emailOutreachTaskDTO.setWeeklyTaskSequence(emailOutreachTask.getWeeklyTaskSequence());

        // Map lifecycle ID if exists
        if (emailOutreachTask.getLifecycle() != null) {
            emailOutreachTaskDTO.setLifecycleId(emailOutreachTask.getLifecycle().getLifecycleId());
        }

        // Map parent task ID if exists
        if (emailOutreachTask.getParentTask() != null) {
            emailOutreachTaskDTO.setParentTaskId(emailOutreachTask.getParentTask().getTaskId());
        }



        if (emailOutreachTask.getRelatedContact() != null) {
            Contact contact = emailOutreachTask.getRelatedContact();
            logger.debug("Contact LinkedIn URL: {}", contact.getLinkedInUrl());
            emailOutreachTaskDTO.setContactId(contact.getContactId());
            emailOutreachTaskDTO.setContactFirstName(contact.getFirstName());
            emailOutreachTaskDTO.setContactLastName(contact.getLastName());
            emailOutreachTaskDTO.setContactEmailID(contact.getEmailID());
            emailOutreachTaskDTO.setContactPhoneNo(contact.getPhoneNo());
            if (contact.getCompany() != null) {
                emailOutreachTaskDTO.setContactCompany(contact.getCompany().getName());
                emailOutreachTaskDTO.setContactCompanyId(contact.getCompany().getCompanyId());
            }

            emailOutreachTaskDTO.setContactCity(contact.getCity());
            emailOutreachTaskDTO.setContactState(contact.getState());
            emailOutreachTaskDTO.setContactCountry(contact.getCountry());
            emailOutreachTaskDTO.setLinkedInUrl(contact.getLinkedInUrl());
            emailOutreachTaskDTO.setDesignation(contact.getDesignation());
        }

        emailOutreachTaskDTO.setType(TaskType.EMAIL_OUTREACH_TASK);

        // Build the task path directly in the method
        String path = "";
        // Resolve lifecycle from current task or walk up to parent
        Lifecycle l = emailOutreachTask.getLifecycle();
        Task current = emailOutreachTask;
        while (l == null && current.getParentTask() != null) {
            current = current.getParentTask();
            l = current.getLifecycle();
        }

        if (l != null) {
            StringBuilder pathBuilder = new StringBuilder();

            if (l.getVersion() != null) {
                Version v = l.getVersion();
                Edition e = v.getEdition();
                MarketingStory s = (e != null ? e.getMarketingStory() : null);
                Collection c = (s != null ? s.getCollection() : null);
                Workspace ws = (c != null ? c.getWorkspace() : null);

                if (ws != null) {
                    System.out.println("WorkspaceName"+ws.getWorkspaceName());
                    pathBuilder.append(ws.getWorkspaceName()).append(" / ");
                }

                if (c != null) {
                    pathBuilder.append(c.getDisplayName()).append(" / ");
                }
                if (s != null) {
                    pathBuilder.append(s.getTitle()).append(" / ");
                }
                if (e != null) {
                    pathBuilder.append(e.getContentType()).append(" / ");
                }
                pathBuilder.append(v.getVersion());
            } else if (l.getCampaign() != null && l.getCampaign().getWorkspace() != null) {
                pathBuilder.append(l.getCampaign().getWorkspace().getWorkspaceName())
                        .append(" / ").append(l.getCampaign().getCampaignName());
            } else if (l.getLead() != null && l.getLead().getCampaign() != null
                    && l.getLead().getCampaign().getWorkspace() != null) {
                pathBuilder.append(l.getLead().getCampaign().getWorkspace().getWorkspaceName())
                        .append(" / ").append(l.getLead().getCampaign().getCampaignName());
            } else if (l.getOpportunity() != null && l.getOpportunity().getWorkspace() != null) {
                pathBuilder.append(l.getOpportunity().getWorkspace().getWorkspaceName())
                        .append(" / ").append(l.getOpportunity().getOpportunityName());
            } else {
                pathBuilder.append("unassigned");
            }

            // Add lead if present
            if (l.getLead() != null) {
                pathBuilder.append("/").append(l.getLead().getLeadName());
            }

            // Add lifecycle
            pathBuilder.append("/").append(l.getLifecycleName());

            // Check if this is a subtask
            if (emailOutreachTask.getParentTask() != null) {
                // Append parent task name only
                pathBuilder.append("/").append(emailOutreachTask.getParentTask().getTaskName());
            }

            // Do NOT append the task's own name
            path = pathBuilder.toString();
        }
        emailOutreachTaskDTO.setPath(path);

        return emailOutreachTaskDTO;

    }

    public LinkedInOutreachTaskDTO transformToLinkedInOutreachTaskDTO(LinkedInOutreachTask linkedInOutreachTask) {
        LinkedInOutreachTaskDTO dto = new LinkedInOutreachTaskDTO();

        // Basic fields
        dto.setTaskId(linkedInOutreachTask.getTaskId());
        dto.setTaskName(linkedInOutreachTask.getTaskName());
        dto.setDescription(linkedInOutreachTask.getDescription());
        dto.setAssignTo(linkedInOutreachTask.getAssignTo());
        dto.setSendToAssignee(linkedInOutreachTask.isSendToAssignee());
        dto.setDuration(linkedInOutreachTask.getDuration());
        dto.setDurationValue(linkedInOutreachTask.getDurationValue());
        dto.setDueDate(linkedInOutreachTask.getDueDate());
        dto.setCreatedOn(linkedInOutreachTask.getCreatedOn());
        dto.setIsSystemComment(linkedInOutreachTask.getIsSystemComment());
        dto.setStatus(linkedInOutreachTask.getStatus());
        dto.setWeeklyTaskSequence(linkedInOutreachTask.getWeeklyTaskSequence());

        // Lifecycle
        if (linkedInOutreachTask.getLifecycle() != null) {
            dto.setLifecycleId(linkedInOutreachTask.getLifecycle().getLifecycleId());
        }

        // Parent Task
        if (linkedInOutreachTask.getParentTask() != null) {
            dto.setParentTaskId(linkedInOutreachTask.getParentTask().getTaskId());
        }

        // Contact Info
        if (linkedInOutreachTask.getRelatedContact() != null) {
            Contact contact = linkedInOutreachTask.getRelatedContact();
            dto.setContactId(contact.getContactId());
            dto.setContactFirstName(contact.getFirstName());
            dto.setContactLastName(contact.getLastName());
            dto.setContactEmailID(contact.getEmailID());
            dto.setContactPhoneNo(contact.getPhoneNo());
            if (contact.getCompany() != null) {
                dto.setContactCompany(contact.getCompany().getName());
                dto.setContactCompanyId(contact.getCompany().getCompanyId());
            }
            dto.setContactCity(contact.getCity());
            dto.setContactState(contact.getState());
            dto.setContactCountry(contact.getCountry());
            dto.setLinkedInUrl(contact.getLinkedInUrl());
            dto.setDesignation(contact.getDesignation());
        }

        dto.setType(TaskType.LINKEDIN_OUTREACH_TASK);

        // Build task path (simplified)
        String path = "";
        Lifecycle lifecycle = linkedInOutreachTask.getLifecycle();
        Task current = linkedInOutreachTask;

        // Climb up to parent if lifecycle is missing
        while (lifecycle == null && current.getParentTask() != null) {
            current = current.getParentTask();
            lifecycle = current.getLifecycle();
        }

        if (lifecycle != null) {
            StringBuilder pathBuilder = new StringBuilder();

            if (lifecycle.getCampaign() != null && lifecycle.getCampaign().getWorkspace() != null) {
                pathBuilder.append(lifecycle.getCampaign().getWorkspace().getWorkspaceName())
                        .append(" / ").append(lifecycle.getCampaign().getCampaignName());
            } else if (lifecycle.getLead() != null && lifecycle.getLead().getCampaign() != null) {
                pathBuilder.append(lifecycle.getLead().getCampaign().getWorkspace().getWorkspaceName())
                        .append(" / ").append(lifecycle.getLead().getCampaign().getCampaignName());
            } else {
                pathBuilder.append("unassigned");
            }

            if (lifecycle.getLead() != null) {
                pathBuilder.append(" / ").append(lifecycle.getLead().getLeadName());
            }

            pathBuilder.append(" / ").append(lifecycle.getLifecycleName());

            if (linkedInOutreachTask.getParentTask() != null) {
                pathBuilder.append(" / ").append(linkedInOutreachTask.getParentTask().getTaskName());
            }

            path = pathBuilder.toString();
        }

        dto.setPath(path);
        return dto;
    }

    public PhoneOutreachTaskDTO transformToPhoneOutreachTaskDTO(PhoneOutreachTask phoneOutreachTask) {
        PhoneOutreachTaskDTO phoneOutreachTaskDTO = new PhoneOutreachTaskDTO();

        phoneOutreachTaskDTO.setTaskId(phoneOutreachTask.getTaskId());
        phoneOutreachTaskDTO.setTaskName(phoneOutreachTask.getTaskName());
        phoneOutreachTaskDTO.setDescription(phoneOutreachTask.getDescription());
        phoneOutreachTaskDTO.setAssignTo(phoneOutreachTask.getAssignTo());
        phoneOutreachTaskDTO.setSendToAssignee(phoneOutreachTask.isSendToAssignee());
        phoneOutreachTaskDTO.setDuration(phoneOutreachTask.getDuration());
        phoneOutreachTaskDTO.setDurationValue(phoneOutreachTask.getDurationValue());
        phoneOutreachTaskDTO.setDueDate(phoneOutreachTask.getDueDate());
        phoneOutreachTaskDTO.setCreatedOn(phoneOutreachTask.getCreatedOn());
        phoneOutreachTaskDTO.setIsSystemComment(phoneOutreachTask.getIsSystemComment());
        phoneOutreachTaskDTO.setStatus(phoneOutreachTask.getStatus());
        phoneOutreachTaskDTO.setWeeklyTaskSequence(phoneOutreachTask.getWeeklyTaskSequence());
        phoneOutreachTaskDTO.setNotReachable(phoneOutreachTask.getNotReachable());
        phoneOutreachTaskDTO.setDidNotAnswer(phoneOutreachTask.getDidNotAnswer());

        // Map lifecycle ID if exists
        if (phoneOutreachTask.getLifecycle() != null) {
            phoneOutreachTaskDTO.setLifecycleId(phoneOutreachTask.getLifecycle().getLifecycleId());
        }

        // Map parent task ID if exists
        if (phoneOutreachTask.getParentTask() != null) {
            phoneOutreachTaskDTO.setParentTaskId(phoneOutreachTask.getParentTask().getTaskId());
        }

        // Map related contact info
        if (phoneOutreachTask.getRelatedContact() != null) {
            Contact contact = phoneOutreachTask.getRelatedContact();
            phoneOutreachTaskDTO.setContactId(contact.getContactId());
            phoneOutreachTaskDTO.setContactFirstName(contact.getFirstName());
            phoneOutreachTaskDTO.setContactLastName(contact.getLastName());
            phoneOutreachTaskDTO.setContactEmailID(contact.getEmailID());
            phoneOutreachTaskDTO.setContactPhoneNo(contact.getPhoneNo());
            if (contact.getCompany() != null) {
                phoneOutreachTaskDTO.setContactCompany(contact.getCompany().getName());
                phoneOutreachTaskDTO.setContactCompanyId(contact.getCompany().getCompanyId());
            }
            phoneOutreachTaskDTO.setContactCity(contact.getCity());
            phoneOutreachTaskDTO.setContactState(contact.getState());
            phoneOutreachTaskDTO.setContactCountry(contact.getCountry());
            phoneOutreachTaskDTO.setLinkedInUrl(contact.getLinkedInUrl());
            phoneOutreachTaskDTO.setDesignation(contact.getDesignation());

        }

        phoneOutreachTaskDTO.setType(TaskType.PHONE_OUTREACH_TASK);

        // Build lifecycle path
        StringBuilder pathBuilder = new StringBuilder();
        Lifecycle l = phoneOutreachTask.getLifecycle();
        Task current = phoneOutreachTask;
        while (l == null && current.getParentTask() != null) {
            current = current.getParentTask();
            l = current.getLifecycle();
        }

        if (l != null) {
            if (l.getCampaign() != null && l.getCampaign().getWorkspace() != null) {
                pathBuilder.append(l.getCampaign().getWorkspace().getWorkspaceName())
                        .append(" / ").append(l.getCampaign().getCampaignName());
            } else if (l.getLead() != null && l.getLead().getCampaign() != null
                    && l.getLead().getCampaign().getWorkspace() != null) {
                pathBuilder.append(l.getLead().getCampaign().getWorkspace().getWorkspaceName())
                        .append(" / ").append(l.getLead().getCampaign().getCampaignName());
            } else if (l.getOpportunity() != null && l.getOpportunity().getWorkspace() != null) {
                pathBuilder.append(l.getOpportunity().getWorkspace().getWorkspaceName())
                        .append(" / ").append(l.getOpportunity().getOpportunityName());
            } else {
                pathBuilder.append("unassigned");
            }

            if (l.getLead() != null) {
                pathBuilder.append("/").append(l.getLead().getLeadName());
            }

            pathBuilder.append("/").append(l.getLifecycleName());

            if (phoneOutreachTask.getParentTask() != null) {
                pathBuilder.append("/").append(phoneOutreachTask.getParentTask().getTaskName());
            }
        }

        phoneOutreachTaskDTO.setPath(pathBuilder.toString());
        return phoneOutreachTaskDTO;
    }


    public TaskDTO transformToTaskDTO(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        // Handle regular Task
        TaskDTO taskDTO = new TaskDTO();

        // Set all common fields
        taskDTO.setTaskId(task.getTaskId());
        taskDTO.setTaskName(task.getTaskName());
        taskDTO.setDescription(task.getDescription());
        taskDTO.setAssignTo(task.getAssignTo());
        taskDTO.setSendToAssignee(task.isSendToAssignee());
        taskDTO.setDuration(task.getDuration());
        taskDTO.setDurationValue(task.getDurationValue());
        taskDTO.setStatus(task.getStatus());
        taskDTO.setWeeklyTaskSequence(task.getWeeklyTaskSequence());
        taskDTO.setIsSystemComment(task.getIsSystemComment());

        // Set nullable fields
        if (task.getDueDate() != null) {
            taskDTO.setDueDate(task.getDueDate());
        }
        if (task.getCreatedOn() != null) {
            taskDTO.setCreatedOn(task.getCreatedOn());
        }

        // Set relationships
        if (task.getLifecycle() != null) {
            taskDTO.setLifecycleId(task.getLifecycle().getLifecycleId());
        }

        if (task.getParentTask() != null) {
            taskDTO.setParentTaskId(task.getParentTask().getTaskId());
        }

        // Handle subtasks
        if (task.getSubTasks() != null && !task.getSubTasks().isEmpty()) {
            List<TaskDTO> subtaskDTOs = task.getSubTasks().stream()
                    .map(this::transformToTaskDTO) // Recursive call
                    .collect(Collectors.toList());
            taskDTO.setSubTasks(subtaskDTOs);
        }

        if (task instanceof EmailOutreachTask) {
            taskDTO.setType(TaskType.EMAIL_OUTREACH_TASK);
        } else if (task instanceof PhoneOutreachTask) {
            taskDTO.setType(TaskType.PHONE_OUTREACH_TASK);
        } else if (task instanceof LinkedInOutreachTask) {
            taskDTO.setType(TaskType.LINKEDIN_OUTREACH_TASK);
        } else {
            taskDTO.setType(TaskType.DEFAULT);
        }

        // Build the task path directly in the method
        String path = "";
        // Resolve lifecycle from current task or walk up to parent
        Lifecycle l = task.getLifecycle();
        Task current = task;
        while (l == null && current.getParentTask() != null) {
            current = current.getParentTask();
            l = current.getLifecycle();
        }

        if (l != null) {
            StringBuilder pathBuilder = new StringBuilder();

            if (l.getVersion() != null) {
                Version v = l.getVersion();
                Edition e = v.getEdition();
                MarketingStory s = (e != null ? e.getMarketingStory() : null);
                Collection c = (s != null ? s.getCollection() : null);
                Workspace ws = (c != null ? c.getWorkspace() : null);

                if (ws != null) {
                    System.out.println("WorkspaceName"+ws.getWorkspaceName());
                    pathBuilder.append(ws.getWorkspaceName()).append(" / ");
                }
                if (c != null) {
                    pathBuilder.append(c.getDisplayName()).append(" / ");
                }
                if (s != null) {
                    pathBuilder.append(s.getTitle()).append(" / ");
                }
                if (e != null) {
                    pathBuilder.append(e.getContentType()).append(" / ");
                }
                pathBuilder.append(v.getVersion());
            } else if (l.getCampaign() != null && l.getCampaign().getWorkspace() != null) {
                pathBuilder.append(l.getCampaign().getWorkspace().getWorkspaceName())
                        .append(" / ").append(l.getCampaign().getCampaignName());
            } else if (l.getLead() != null && l.getLead().getCampaign() != null
                    && l.getLead().getCampaign().getWorkspace() != null) {
                pathBuilder.append(l.getLead().getCampaign().getWorkspace().getWorkspaceName())
                        .append(" / ").append(l.getLead().getCampaign().getCampaignName());
            } else if (l.getOpportunity() != null && l.getOpportunity().getWorkspace() != null) {
                pathBuilder.append(l.getOpportunity().getWorkspace().getWorkspaceName())
                        .append(" / ").append(l.getOpportunity().getOpportunityName());
            } else {
                pathBuilder.append("unassigned");
            }




            // Add lead if present
            if (l.getLead() != null) {
                pathBuilder.append("/").append(l.getLead().getLeadName());
            }

            // Add lifecycle
            pathBuilder.append("/").append(l.getLifecycleName());

            // Check if this is a subtask
            if (task.getParentTask() != null) {
                // Append parent task name only
                pathBuilder.append("/").append(task.getParentTask().getTaskName());
            }

            // Do NOT append the task's own name
            path = pathBuilder.toString();
        }
        taskDTO.setPath(path);
        return taskDTO;
    }


    public List<UserTaskSequence> updateTaskSequence(String userName, List<TaskDTO> orderedTaskList) {
        int taskLength = orderedTaskList.size();
        List<UserTaskSequence> sequencesToSave = new ArrayList<>();

        // Extract task IDs from incoming task list
        List<Long> taskIds = orderedTaskList.stream()
                .map(TaskDTO::getTaskId)
                .toList();

        // Fetch existing user-task mappings from DB
        List<UserTaskSequence> existingSequences = userTaskSequenceRepository
                .findByUserNameAndTask_TaskIdIn(userName, taskIds);

        // Map of taskId -> existing UserTaskSequence
        Map<Long, UserTaskSequence> sequenceMap = existingSequences.stream()
                .collect(Collectors.toMap(seq -> seq.getTask().getTaskId(), seq -> seq));

        // Loop over the ordered list and assign sequence numbers
        for (int i = 0; i < taskLength; i++) {
            Long taskId = orderedTaskList.get(i).getTaskId();
            int newSequenceOrder = i + 1; // sequence starts from 1

            UserTaskSequence existingSequence = sequenceMap.get(taskId);

            if (existingSequence != null) {
                // Update sequence if changed
                if (existingSequence.getSequenceOrder() != newSequenceOrder) {
                    existingSequence.setSequenceOrder(newSequenceOrder);
                    sequencesToSave.add(existingSequence);
                }
            } else {
                // Create new sequence if none exists
                Optional<Task> taskOpt = taskRepository.findById(taskId);
                if (taskOpt.isPresent()) {
                    UserTaskSequence newSequence = new UserTaskSequence();
                    newSequence.setUserName(userName);
                    newSequence.setTask(taskOpt.get());
                    newSequence.setSequenceOrder(newSequenceOrder);
                    sequencesToSave.add(newSequence);
                }
            }
        }

        // Save all changes at once if there are any
        if (!sequencesToSave.isEmpty()) {
            userTaskSequenceRepository.saveAll(sequencesToSave);
        }

        return sequencesToSave;
    }

    public List<TaskWeeklyPlannerDTO> getDailyTasks(
            String assignTo,
            boolean sendToAssignee,
            int year,
            int month,
            int day,boolean excludeOutreachTasks) {

        // Construct start and end of that specific day in GMT/UTC
        long Day = getStartOfDayGMT(year, month, day);


        // Reuse your existing fetch logic
        List<TaskWeeklyPlannerDTO> tasks = fetchAndEnrichTasks(assignTo, sendToAssignee, 0L, Day);
        if (excludeOutreachTasks){
            tasks = tasks.stream()
                    .filter(dto -> !isOutreachType(dto.getType()))//here we exclude outreach type tasks.
                    .collect(Collectors.toList());
        }
        return tasks;
    }

    private boolean isOutreachType(String type) {
        return TaskType.EMAIL_OUTREACH_TASK.equalsIgnoreCase(type)
                || TaskType.PHONE_OUTREACH_TASK.equalsIgnoreCase(type)
                || TaskType.LINKEDIN_OUTREACH_TASK.equalsIgnoreCase(type);
    }

    public long getStartOfDayGMT(int year, int month, int day) {
        LocalDate userDate = LocalDate.of(year, month, day);
        return userDate.atTime(1, 0, 0) // day starts at 1 AM
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
    }

    // WEEKLY PLANNER
    public List<TaskWeeklyPlannerDTO> getWeeklyTasks(
            String assignTo,
            boolean sendToAssignee,
            int year,
            int month,
            int weekStartDay,
            int todayYear,
            int todayMonth,
            int todayDay) {

        LocalDate startDate = LocalDate.of(year, month, weekStartDay);
        LocalDate endDate = startDate.plusDays(6);

        long startOfWeek = startDate.atTime(1, 0, 0).atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        long endOfWeek = endDate.atTime(1, 0, 0).atZone(ZoneOffset.UTC).toInstant().toEpochMilli();

        long today = getStartOfDayGMT(todayYear, todayMonth, todayDay);

        // Case A: entire week is in the past
        if (endOfWeek < today) {
            return Collections.emptyList();
        }

        // Case B: entire week is in the future
        if (startOfWeek > today) {
            return getFutureTasks(assignTo, sendToAssignee, startOfWeek, endOfWeek);
        }

        // Case C: week includes today
        List<TaskWeeklyPlannerDTO> tasks = new ArrayList<>();

        // always include today
        tasks.addAll(getTodayTasks(today, assignTo, sendToAssignee));

        // add future tasks if week extends beyond today
        if (endOfWeek > today) {
            tasks.addAll(getFutureTasks(assignTo, sendToAssignee, today + 1, endOfWeek));
        }

        return tasks;
    }

    private List<TaskWeeklyPlannerDTO> getTodayTasks(long today, String assignTo, boolean sendToAssignee) {
        return fetchAndEnrichTasks(assignTo, sendToAssignee, 0L, today);
    }


    private List<TaskWeeklyPlannerDTO> getFutureTasks(
            String assignTo,
            boolean sendToAssignee,
            long startTime,
            long endTime) {

        return fetchAndEnrichTasks(assignTo, sendToAssignee, startTime, endTime);
    }

    private List<TaskWeeklyPlannerDTO> fetchAndEnrichTasks(
            String assignTo,
            boolean sendToAssignee,
            long startTime,
            long endTime) {

        List<TaskWeeklyPlannerDTO> tasks = taskRepository.getTasksByAssignToAndSendToAssigneeAndDueDateRange(
                assignTo, sendToAssignee, startTime, endTime);

        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }

        tasks = filterOutFinalStageTasks(tasks);
        enrichTasks(tasks);

        return tasks;
    }

    private List<TaskWeeklyPlannerDTO> filterOutFinalStageTasks(List<TaskWeeklyPlannerDTO> tasks) {
        String outreachFinalStage = getFinalStageStatus(5L);
        String defaultFinalStage = getFinalStageStatus(6L);

        return tasks.stream()
                //no matter what outreach subtype (Email, LinkedIn, Phone) — if the status = "Completed", it gets removed.
                .filter(task -> !outreachFinalStage.equalsIgnoreCase(task.getStatus()) &&
                        !defaultFinalStage.equalsIgnoreCase(task.getStatus()))
                .collect(Collectors.toList());
    }

    private void enrichTasks(List<TaskWeeklyPlannerDTO> tasks) {
        for (TaskWeeklyPlannerDTO dto : tasks) {
            Task fullTask = taskRepository.findById(dto.getTaskId())
                    .orElseThrow(() -> new RuntimeException("Task not found: " + dto.getTaskId()));

            // we override path which comes from JPA query here.
            if (dto.getPath() == null
                    || dto.getPath().equalsIgnoreCase("Unknown")
                    || dto.getPath().equalsIgnoreCase("unassigned")
                    || dto.getPath().equalsIgnoreCase("Unknown / Planning")) {
                dto.setPath(buildTaskPath(fullTask));
            }

            if (fullTask instanceof EmailOutreachTask emailOutreachTask) {
                dto.setType(TaskType.EMAIL_OUTREACH_TASK);
                if (emailOutreachTask.getRelatedContact() != null) {
                    Contact c = emailOutreachTask.getRelatedContact();
                    dto.setContactId(c.getContactId());
                    dto.setContactFirstName(c.getFirstName());
                    dto.setContactLastName(c.getLastName());
                    dto.setContactEmailID(c.getEmailID());
                    dto.setContactPhoneNo(c.getPhoneNo());
                    dto.setContactCompany(c.getCompany() != null ? c.getCompany().getName() : null);
                    dto.setContactCity(c.getCity());
                    dto.setContactState(c.getState());
                    dto.setContactCountry(c.getCountry());
                    dto.setLinkedInUrl(c.getLinkedInUrl());
                }
            }
            else if (fullTask instanceof PhoneOutreachTask phoneOutreachTask) {
                dto.setType(TaskType.PHONE_OUTREACH_TASK);
                if (phoneOutreachTask.getRelatedContact() != null) {
                    Contact c = phoneOutreachTask.getRelatedContact();
                    dto.setContactId(c.getContactId());
                    dto.setContactFirstName(c.getFirstName());
                    dto.setContactLastName(c.getLastName());
                    dto.setContactEmailID(c.getEmailID());
                    dto.setContactPhoneNo(c.getPhoneNo());
                    dto.setContactCompany(c.getCompany() != null ? c.getCompany().getName() : null);
                    dto.setContactCity(c.getCity());
                    dto.setContactState(c.getState());
                    dto.setContactCountry(c.getCountry());
                    dto.setLinkedInUrl(c.getLinkedInUrl());
                }
            } else if (fullTask instanceof LinkedInOutreachTask linkedInOutreachTask) {
                dto.setType(TaskType.LINKEDIN_OUTREACH_TASK);
                if (linkedInOutreachTask.getRelatedContact() != null) {
                    Contact c = linkedInOutreachTask.getRelatedContact();
                    dto.setContactId(c.getContactId());
                    dto.setContactFirstName(c.getFirstName());
                    dto.setContactLastName(c.getLastName());
                    dto.setContactEmailID(c.getEmailID());
                    dto.setContactPhoneNo(c.getPhoneNo());
                    dto.setContactCompany(c.getCompany() != null ? c.getCompany().getName() : null);
                    dto.setContactCity(c.getCity());
                    dto.setContactState(c.getState());
                    dto.setContactCountry(c.getCountry());
                    dto.setLinkedInUrl(c.getLinkedInUrl());
                }
            } else {
                dto.setType(TaskType.DEFAULT);
            }

        }
    }

    public String getFinalStageStatus(Long cycleId) {
        List<ConstantLifecycle> stages = constantLifecycleRepository.findByCycleId(cycleId);
        if (stages.isEmpty()) {
            throw new IllegalStateException("No lifecycle stages defined for cycleId: " + cycleId);
        }
        return stages.get(stages.size() - 1).getCycleName();
    }

    private String buildTaskPath(Task task) {
        Lifecycle l = task.getLifecycle();
        Task current = task;

        // walk up to parent if lifecycle is null
        while (l == null && current.getParentTask() != null) {
            current = current.getParentTask();
            l = current.getLifecycle();
        }

        if (l == null) {
            return "unassigned";
        }

        StringBuilder pathBuilder = new StringBuilder();

        if (l.getVersion() != null) {
            Version v = l.getVersion();
            Edition e = v.getEdition();
            MarketingStory s = (e != null ? e.getMarketingStory() : null);
            Collection c = (s != null ? s.getCollection() : null);
            Workspace ws = (c != null ? c.getWorkspace() : null);

            if (ws != null) {
                System.out.println("WorkspaceName: " + ws.getWorkspaceName());
                pathBuilder.append(ws.getWorkspaceName()).append(" / ");
            }
            if (c != null) {
                pathBuilder.append(c.getDisplayName()).append(" / ");
            }
            if (s != null) {
                pathBuilder.append(s.getTitle()).append(" / ");
            }
            if (e != null) {
                pathBuilder.append(e.getContentType()).append(" / ");
            }
            pathBuilder.append(v.getVersion());
        } else if (l.getCampaign() != null && l.getCampaign().getWorkspace() != null) {
            pathBuilder.append(l.getCampaign().getWorkspace().getWorkspaceName())
                    .append(" / ").append(l.getCampaign().getCampaignName());
        } else if (l.getLead() != null && l.getLead().getCampaign() != null
                && l.getLead().getCampaign().getWorkspace() != null) {
            pathBuilder.append(l.getLead().getCampaign().getWorkspace().getWorkspaceName())
                    .append(" / ").append(l.getLead().getCampaign().getCampaignName());
        } else if (l.getOpportunity() != null && l.getOpportunity().getWorkspace() != null) {
            pathBuilder.append(l.getOpportunity().getWorkspace().getWorkspaceName())
                    .append(" / ").append(l.getOpportunity().getOpportunityName());
        } else {
            pathBuilder.append("unassigned");
        }

        // Lead
        if (l.getLead() != null) {
            pathBuilder.append(" / ").append(l.getLead().getLeadName());
        }

        // Lifecycle
        pathBuilder.append(" / ").append(l.getLifecycleName());

        // Parent task name (only once, not the full chain)
        if (task.getParentTask() != null) {
            pathBuilder.append(" / ").append(task.getParentTask().getTaskName());
        }

        return pathBuilder.toString();
    }


}
