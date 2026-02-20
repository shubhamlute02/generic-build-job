package com.accrevent.radius.repository;

import com.accrevent.radius.model.SelectedLifecycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SelectedLifecycleRepository extends JpaRepository<SelectedLifecycle, Long> {
    Optional<SelectedLifecycle> findByType(String type);
}
