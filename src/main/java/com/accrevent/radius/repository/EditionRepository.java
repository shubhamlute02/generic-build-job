package com.accrevent.radius.repository;

import com.accrevent.radius.model.Edition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EditionRepository extends JpaRepository<Edition,Long> {
}
