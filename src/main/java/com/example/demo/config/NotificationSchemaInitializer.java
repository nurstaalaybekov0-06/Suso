package com.example.demo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class NotificationSchemaInitializer {

    @Bean
    CommandLineRunner ensureNotificationReadColumn(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("ALTER TABLE notification ADD COLUMN IF NOT EXISTS read BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute("UPDATE notification SET read = FALSE WHERE read IS NULL");
            jdbcTemplate.execute("ALTER TABLE notification ALTER COLUMN read SET DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE notification ALTER COLUMN read SET NOT NULL");
        };
    }
}
