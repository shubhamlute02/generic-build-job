package com.accrevent.radius.service;

import com.accrevent.radius.Serializable.UserTaskCommentTimeTrack;
import com.accrevent.radius.exception.ResourceNotFoundException;
import com.accrevent.radius.model.TaskCommentTrack;
import com.accrevent.radius.repository.TaskCommentTrackRepository;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Optional;
@Service
public class TaskCommentTrackService {

    private final TaskCommentTrackRepository taskCommentTrackRepository;

    public TaskCommentTrackService(TaskCommentTrackRepository taskCommentTrackRepository) {
        this.taskCommentTrackRepository = taskCommentTrackRepository;
    }


    public TaskCommentTrack createOrUpdateTaskCommentTrack(TaskCommentTrack taskCommentTrack) {
        if (taskCommentTrackRepository.existsById(new UserTaskCommentTimeTrack(taskCommentTrack.getUserId(), taskCommentTrack.getTaskId()))) {
            // Update the existing record
            taskCommentTrack.setDateTime(ZonedDateTime.now());
            TaskCommentTrack savedTask = taskCommentTrackRepository.save(taskCommentTrack);
            return savedTask;
        }
        else
        {
            // Create a new record if it doesn't exist
            taskCommentTrack.setDateTime(ZonedDateTime.now());
            return taskCommentTrackRepository.save(taskCommentTrack);
        }
    }

    public TaskCommentTrack getUserTaskCommentTrack(String userId, Long taskId) {
        Optional<TaskCommentTrack> taskCommentTrack = taskCommentTrackRepository.findById(new UserTaskCommentTimeTrack(userId, taskId));
        if(taskCommentTrack.isPresent()) {
            return taskCommentTrack.get();
        }
        return null;
    }


}
