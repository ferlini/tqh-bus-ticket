package com.tqh.bus.ticket.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "tqh.base-url=https://custom.api.com",
        "tqh.auth-token=custom-token",
        "tqh.route-id=100",
        "tqh.boarding-point-id=50",
        "tqh.alighting-point-id=200",
        "tqh.monitor-interval=60"
})
class TqhPropertiesCustomValuesTest {

    @Autowired
    private TqhProperties tqhProperties;

    @Test
    void should_bind_custom_values() {
        assertThat(tqhProperties.getBaseUrl()).isEqualTo("https://custom.api.com");
        assertThat(tqhProperties.getAuthToken()).isEqualTo("custom-token");
        assertThat(tqhProperties.getRouteId()).isEqualTo(100);
        assertThat(tqhProperties.getBoardingPointId()).isEqualTo(50);
        assertThat(tqhProperties.getAlightingPointId()).isEqualTo(200);
        assertThat(tqhProperties.getMonitorInterval()).isEqualTo(60);
    }
}
