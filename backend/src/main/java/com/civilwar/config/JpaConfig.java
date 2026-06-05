package com.civilwar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Profile("oracle")
@EnableJpaRepositories(basePackages = "com.civilwar.domain.repository")
public class JpaConfig {
}
