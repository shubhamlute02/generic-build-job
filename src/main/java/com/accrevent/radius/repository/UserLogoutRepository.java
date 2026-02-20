package com.accrevent.radius.repository;

import com.accrevent.radius.model.UserLogout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLogoutRepository extends JpaRepository<UserLogout, Long> {
    Optional<UserLogout> findByUserId(String userId);
}
