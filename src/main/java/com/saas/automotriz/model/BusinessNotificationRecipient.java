package com.saas.automotriz.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "business_notification_recipients", uniqueConstraints = @UniqueConstraint(columnNames = {"notification_id", "business_id"}))
public class BusinessNotificationRecipient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "notification_id", nullable = false)
    private PlatformNotification notification;

    @ManyToOne @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    private Boolean dismissed = false;
}
