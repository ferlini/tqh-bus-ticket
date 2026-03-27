package com.tqh.bus.ticket.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TqhPropertiesTest {

    @Autowired
    private TqhProperties tqhProperties;

    @Test
    void should_bind_properties_from_yml() {
        assertThat(tqhProperties.getBaseUrl()).isNotBlank();
        assertThat(tqhProperties.getAuthToken()).isNotBlank();
        assertThat(tqhProperties.getRouteId()).isPositive();
        assertThat(tqhProperties.getBoardingPointId()).isPositive();
        assertThat(tqhProperties.getAlightingPointId()).isPositive();
        assertThat(tqhProperties.getMonitorInterval()).isPositive();
    }
}
