package com.accrevent.radius.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public class TaskCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public int bulkUpdateDueDates(List<Long> taskIds, Long dueDate) {
        return entityManager.createQuery("UPDATE Task t SET t.dueDate = :dueDate WHERE t.taskId IN :taskIds")
                .setParameter("dueDate", dueDate)
                .setParameter("taskIds", taskIds)
                .executeUpdate();
    }
}
