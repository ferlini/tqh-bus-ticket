package com.tqh.bus.ticket.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tqh.bus.ticket.config.OpenClawWebhookProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.Map;

@Component
public class OpenClawWebhookClient {

    private static final Logger log = LoggerFactory.getLogger(OpenClawWebhookClient.class);
    private static final String SEND_MODE = "now";

    private final OpenClawWebhookProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenClawWebhookClient(OpenClawWebhookProperties properties) {
        this.properties = properties;
        // 强制 HTTP/1.1：JDK HttpClient 默认 HTTP/2，对明文 http 端点会附带 h2c upgrade 头，
        // 部分简易 webhook 网关（如 openclaw）会回 405 "Invalid HTTP method"。
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
        this.objectMapper = new ObjectMapper();
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
        String text = "给 " + properties.getChannel() + " 发送内容并适当的美化：你好，刚买了一张车票，记得付款。\n" + purchaseMessage;
        return Map.of("text", text, "mode", SEND_MODE);
    }

    public boolean notifyTicketAvailable(String availabilityMessage) {
        Map<String, String> payload = buildAvailabilityPayload(availabilityMessage);
        try {
            String body = restClient.post()
                    .uri(properties.getUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            boolean ok = isOkResponse(body);
            if (ok) {
                log.info("OpenClaw 有票通知已成功发送, channel={}", properties.getChannel());
            } else {
                log.warn("OpenClaw 有票通知响应非 ok=true, body={}", body);
            }
            return ok;
        } catch (Exception e) {
            log.error("OpenClaw 有票通知发送失败: {}", e.getMessage(), e);
            return false;
        }
    }

    Map<String, String> buildAvailabilityPayload(String availabilityMessage) {
        String text = "给 " + properties.getChannel() + " 发送内容并适当的美化：发现以下日期有车票可购买：\n" + availabilityMessage;
        return Map.of("text", text, "mode", SEND_MODE);
    }

    boolean isOkResponse(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            return node.path("ok").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }
}
