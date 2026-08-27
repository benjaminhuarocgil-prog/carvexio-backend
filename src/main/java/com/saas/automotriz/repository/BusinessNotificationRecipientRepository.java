package com.saas.automotriz.repository;

import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.BusinessNotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BusinessNotificationRecipientRepository extends JpaRepository<BusinessNotificationRecipient, Long> {
    List<BusinessNotificationRecipient> findByBusinessOrderByNotificationCreatedAtDesc(Business business);
    long countByBusinessAndDismissedFalse(Business business);
}
