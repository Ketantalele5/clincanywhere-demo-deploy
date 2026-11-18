package com.clincanywhere.repository;

import com.clincanywhere.entity.PatientInvite;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.QueryHint;
import org.springframework.data.repository.query.Param;

public interface PatientInviteRepository extends JpaRepository<PatientInvite, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select i from PatientInvite i where i.tokenHash = :hash")
    Optional<PatientInvite> findByTokenHashForUpdate(@Param("hash") String hash);
}
