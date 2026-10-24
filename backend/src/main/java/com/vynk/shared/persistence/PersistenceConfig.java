package com.vynk.shared.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Persistence configuration for the Vynk backend.
 */
@Configuration
@EnableJpaAuditing
public class PersistenceConfig {
}
