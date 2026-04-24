package com.tqh.bus.ticket.integration;

import com.tqh.bus.ticket.config.OpenClawWebhookProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class OpenClawWebhookClient {

    private static final Logger log = LoggerFactory.getLogger(OpenClawWebhookClient.class);
    private static final String SEND_MODE = "now";

    private final OpenClawWebhookProperties properties;
    private final RestClient restClient;

    public OpenClawWebhookClient(OpenClawWebhookProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public void notifyTicketPurchase(String purchaseMessage) {
        Map<String, String> payload = buildPayload(purchaseMessage);
        try {
            restClient.post()
                    .uri(properties.getUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("OpenClaw webhook 通知已发送, channel={}", properties.getChannel());
        } catch (Exception e) {
            // 购票主流程已成功，webhook 仅用于通知。降级处理：记录失败但不中断调用方。
            log.error("OpenClaw webhook 通知失败: {}", e.getMessage(), e);
        }
    }

    Map<String, String> buildPayload(String purchaseMessage) {
        String text = "给 " + properties.getChannel() + " 发送：" + purchaseMessage;
        return Map.of("text", text, "mode", SEND_MODE);
    }
}
