package com.clincanywhere.repository;

import com.clincanywhere.entity.UserNotification;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findByClinicIdAndRecipientOrderByIdDesc(String clinicId, String recipient, Pageable pageable);

    List<UserNotification> findByClinicIdAndRecipientAndIdGreaterThanOrderByIdAsc(
            String clinicId, String recipient, Long afterId, Pageable pageable);

    long deleteByCreatedAtBefore(Instant cutoff);
}
