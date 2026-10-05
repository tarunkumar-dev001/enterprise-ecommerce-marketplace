package com.ecommerce.marketplace.config;

import com.ecommerce.marketplace.service.DemoDataSeedService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true")
public class DemoDataConfiguration {

    @Bean
    ApplicationRunner demoDataRunner(DemoDataSeedService seedService) {
        return args -> seedService.seed();
    }
}
