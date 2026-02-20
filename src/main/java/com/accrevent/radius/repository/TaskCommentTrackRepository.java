package com.accrevent.radius.repository;

import com.accrevent.radius.Serializable.UserTaskCommentTimeTrack;
import com.accrevent.radius.model.TaskCommentTrack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCommentTrackRepository extends JpaRepository<TaskCommentTrack, UserTaskCommentTimeTrack>
{

}
