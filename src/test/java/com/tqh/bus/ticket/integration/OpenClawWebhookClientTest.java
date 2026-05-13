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
        properties.setUrl("http://127.0.0.1:1/hooks/agent");
        properties.setToken("test-token");
        properties.setName("main");
        properties.setChannel("openclaw-weixin");
        properties.setTarget("b263ba753a2e-im-bot");
        client = new OpenClawWebhookClient(properties);
    }

    @Test
    void should_wrap_message_with_target_prefix_in_message_field() {
        // given
        String purchaseMessage = "----------------------------------------\n日期: 2026/3/25\n";

        // when
        Map<String, String> payload = client.buildPayload(purchaseMessage);

        // then
        assertThat(payload.get("message"))
                .isEqualTo("给 b263ba753a2e-im-bot 发送内容并适当的美化："
                        + "你好，刚买了一张车票，记得付款。\n" + purchaseMessage);
        assertThat(payload.get("name")).isEqualTo("main");
        assertThat(payload.get("channel")).isEqualTo("openclaw-weixin");
    }

    @Test
    void should_swallow_exception_when_webhook_unreachable() {
        // given: properties.url points to an unreachable port (port 1)

        // when & then: failure must not propagate so purchase flow is preserved
        assertThatCode(() -> client.notifyTicketPurchase("test message"))
                .doesNotThrowAnyException();
    }

    @Test
    void should_build_availability_payload_with_target_prefix() {
        // given
        String availabilityMessage = "线路A\n  - 2026-05-02: 剩余10张";

        // when
        Map<String, String> payload = client.buildAvailabilityPayload(availabilityMessage);

        // then
        assertThat(payload.get("message"))
                .isEqualTo("给 b263ba753a2e-im-bot 发送内容并适当的美化："
                        + "发现以下日期有车票可购买：\n" + availabilityMessage);
        assertThat(payload.get("name")).isEqualTo("main");
        assertThat(payload.get("channel")).isEqualTo("openclaw-weixin");
    }

    @Test
    void should_return_true_when_response_body_indicates_ok() {
        assertThat(client.isOkResponse("{\"ok\": true}")).isTrue();
        assertThat(client.isOkResponse("{\"ok\":true,\"id\":\"abc\"}")).isTrue();
    }

    @Test
    void should_return_false_when_response_body_indicates_not_ok() {
        assertThat(client.isOkResponse("{\"ok\": false}")).isFalse();
        assertThat(client.isOkResponse("{\"status\": \"error\"}")).isFalse();
    }

    @Test
    void should_return_false_when_response_body_invalid() {
        assertThat(client.isOkResponse(null)).isFalse();
        assertThat(client.isOkResponse("")).isFalse();
        assertThat(client.isOkResponse("not-json")).isFalse();
    }

    @Test
    void should_return_false_when_availability_webhook_unreachable() {
        // given: properties.url points to an unreachable port

        // when & then: error path must return false rather than throwing
        assertThat(client.notifyTicketAvailable("test message")).isFalse();
    }
}
