package com.accrevent.radius.repository;

import com.accrevent.radius.model.UserRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface UserRegionRepository extends JpaRepository<UserRegion,Long> {
    boolean existsByUserId(String userId);
    List<UserRegion> findByRegion(String region);
    UserRegion findByUserId(String userId);
}
