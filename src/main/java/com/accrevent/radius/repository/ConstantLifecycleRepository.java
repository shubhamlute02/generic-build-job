package com.accrevent.radius.repository;

import com.accrevent.radius.model.ConstantLifecycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ConstantLifecycleRepository extends JpaRepository<ConstantLifecycle,Long>
{

    @Query("SELECT c FROM ConstantLifecycle c WHERE c.cycleId = :cycleId ORDER BY c.constantLifecycleId ASC")
    List<ConstantLifecycle> findByCycleId(@Param("cycleId") Long cycleId);

    @Transactional
    void deleteByCycleId(Long cycleId);




}
