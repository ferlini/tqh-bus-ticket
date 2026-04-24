package com.tqh.bus.ticket.integration;

import com.tqh.bus.ticket.config.OpenClawWebhookProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class OpenClawWebhookClientTest {

    private OpenClawWebhookProperties properties;
    private OpenClawWebhookClient client;

    @BeforeEach
    void setUp() {
        properties = new OpenClawWebhookProperties();
        properties.setUrl("http://127.0.0.1:1/hooks/wake");
        properties.setToken("test-token");
        properties.setChannel("openclaw-weixin b263ba753a2e-im-bot");
        client = new OpenClawWebhookClient(properties);
    }

    @Test
    void should_wrap_message_with_channel_prefix_in_text_field() {
        // given
        String purchaseMessage = "----------------------------------------\n日期: 2026/3/25\n";

        // when
        Map<String, String> payload = client.buildPayload(purchaseMessage);

        // then
        assertThat(payload.get("text"))
                .isEqualTo("给 openclaw-weixin b263ba753a2e-im-bot 发送：" + purchaseMessage);
        assertThat(payload.get("mode")).isEqualTo("now");
    }

    @Test
    void should_swallow_exception_when_webhook_unreachable() {
        // given: properties.url points to an unreachable port (port 1)

        // when & then: failure must not propagate so purchase flow is preserved
        assertThatCode(() -> client.notifyTicketPurchase("test message"))
                .doesNotThrowAnyException();
    }
}
