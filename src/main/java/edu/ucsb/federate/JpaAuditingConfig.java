package edu.ucsb.federate;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@Configuration
public class JpaAuditingConfig {

  @Bean
  public DateTimeProvider dateTimeProvider() {
    return () -> Optional.of(java.time.Instant.now());
  }
}
