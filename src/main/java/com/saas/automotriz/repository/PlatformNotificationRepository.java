package com.saas.automotriz.repository;

import com.saas.automotriz.model.PlatformNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlatformNotificationRepository extends JpaRepository<PlatformNotification, Long> {
    List<PlatformNotification> findAllByOrderByCreatedAtDesc();
}
