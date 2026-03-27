package com.tqh.bus.ticket.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RestClientConfigTest {

    @Autowired
    private RestClient restClient;

    @Test
    void should_create_rest_client_bean() {
        assertThat(restClient).isNotNull();
    }
}
