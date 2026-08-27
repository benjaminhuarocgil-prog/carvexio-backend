package com.saas.automotriz.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "platform_settings")
public class PlatformSettings {

    /** Singleton row (id=1) containing the commercial settings of the platform. */
    @Id
    private Long id;

    /** Percentage retained by the platform for new marketplace orders. */
    private Integer marketplaceCommissionRate = 20;
}
