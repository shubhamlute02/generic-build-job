package com.accrevent.radius.repository;

import com.accrevent.radius.model.Version;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VersionRepository extends JpaRepository<Version,Long> {

    @Transactional
    @Modifying
    @Query("UPDATE Version v SET v.status = :status, v.approvedAt = :approvedAt WHERE v.versionId = :versionId")
    int updateVersionStatus(@Param("versionId") Long versionId,
                            @Param("status") String status,
                            @Param("approvedAt") Long approvedAt);


    int countByEditionId(Long editionId);

    List<Version> findByEditionId(Long editionId);

    Optional<Version> findTopByEditionIdOrderByVersionIdDesc(Long editionId);

}
