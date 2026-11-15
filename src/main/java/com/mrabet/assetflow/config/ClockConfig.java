package com.mrabet.assetflow.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Horloge injectée : le code métier ne lit jamais l'heure système directement, ce qui le rend testable. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${assetflow.timezone:Europe/Paris}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
