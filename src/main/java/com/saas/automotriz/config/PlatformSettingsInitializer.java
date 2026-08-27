package com.saas.automotriz.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Keeps the singleton settings table available in deployments that do not use
 * Hibernate schema auto-update or a migration tool yet.
 */
@Configuration
public class PlatformSettingsInitializer {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    ApplicationRunner createPlatformSettingsTable(TransactionTemplate transactionTemplate) {
        return args -> transactionTemplate.executeWithoutResult(status -> {
            entityManager.createNativeQuery("""
                    CREATE TABLE IF NOT EXISTS platform_settings (
                        id BIGINT PRIMARY KEY,
                        marketplace_commission_rate INTEGER NOT NULL DEFAULT 20
                    )
                    """).executeUpdate();

            // Render no aplica cambios de entidades a PostgreSQL automáticamente.
            // Estas columnas son necesarias antes de que Hibernate pueda leer cualquier pedido.
            entityManager.createNativeQuery("""
                    ALTER TABLE orders
                    ADD COLUMN IF NOT EXISTS platform_commission_rate INTEGER
                    """).executeUpdate();
            entityManager.createNativeQuery("""
                    ALTER TABLE orders
                    ADD COLUMN IF NOT EXISTS platform_commission_amount DOUBLE PRECISION
                    """).executeUpdate();
            entityManager.createNativeQuery("""
                    ALTER TABLE orders
                    ADD COLUMN IF NOT EXISTS business_payout_amount DOUBLE PRECISION
                    """).executeUpdate();
        });
    }
}
