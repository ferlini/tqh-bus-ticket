package com.tqh.bus.ticket.service;

import com.tqh.bus.ticket.common.BusinessException;
import com.tqh.bus.ticket.config.TqhProperties;
import com.tqh.bus.ticket.integration.TqhApiClient;
import com.tqh.bus.ticket.integration.model.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Component
public class TicketLogService {

    private static final Logger log = LoggerFactory.getLogger(TicketLogService.class);
    private static final String SEPARATOR = "----------------------------------------\n";

    // API 查询配置常量
    private static final int DEFAULT_ORDER_PAGE = 1;
    private static final int DEFAULT_ORDER_PAGE_SIZE = 10;

    private final TqhApiClient apiClient;
    private final TqhProperties properties;
    private final Path logFilePath;

    public TicketLogService(TqhApiClient apiClient, TqhProperties properties) {
        this.apiClient = apiClient;
        this.properties = properties;
        this.logFilePath = Path.of(properties.getLogFilePath());
    }

    public void logTicketPurchase(int wxOrderId) {
        List<OrderItem> orders = apiClient.getOrders("", DEFAULT_ORDER_PAGE, DEFAULT_ORDER_PAGE_SIZE);
        OrderItem targetOrder = orders.stream()
                .filter(order -> order.getId() == wxOrderId)
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到 wx_order_id=" + wxOrderId + " 的订单"));

        String entry = formatLogEntry(targetOrder);
        writeToFile(entry);
    }

    public void writeUnpaidOrderWarning(String message) {
        writeToFile(SEPARATOR + message + "\n");
    }

    @Scheduled(cron = "${tqh.log-clear-cron:0 0 10 ? * FRI}")
    public void clearLog() {
        try {
            if (Files.exists(logFilePath)) {
                Files.writeString(logFilePath, "");
            }
            log.info("购票日志已清空");
        } catch (IOException e) {
            throw new BusinessException("清空日志失败: " + e.getMessage());
        }
    }

    private String formatLogEntry(OrderItem order) {
        String date = order.getDescription().getDate().get(0);
        return SEPARATOR
                + "日期: " + date + "\n"
                + "路线: " + order.getRouteName() + "\n"
                + "上车站: " + order.getDescription().getStartStop() + "\n"
                + "下车站: " + order.getDescription().getEndStop() + "\n";
    }

    private void writeToFile(String content) {
        try {
            Files.createDirectories(logFilePath.getParent());
            Files.writeString(logFilePath, content,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("购票日志已写入: {}", logFilePath);
        } catch (IOException e) {
            throw new BusinessException("写入购票日志失败: " + e.getMessage());
        }
    }
}
