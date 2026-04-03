package com.tqh.bus.ticket.service;

import com.tqh.bus.ticket.common.UnpaidOrderException;
import com.tqh.bus.ticket.config.TqhProperties;
import com.tqh.bus.ticket.integration.TqhApiClient;
import com.tqh.bus.ticket.integration.model.ScheduleItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class TicketMonitorService {

    private static final Logger log = LoggerFactory.getLogger(TicketMonitorService.class);
    private static final DateTimeFormatter SCHEDULE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/M/d");

    private final TqhApiClient apiClient;
    private final OrderService orderService;
    private final TicketLogService ticketLogService;
    private final TqhProperties properties;

    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> currentTask;

    public TicketMonitorService(TqhApiClient apiClient,
                                OrderService orderService,
                                TicketLogService ticketLogService,
                                TqhProperties properties) {
        this.apiClient = apiClient;
        this.orderService = orderService;
        this.ticketLogService = ticketLogService;
        this.properties = properties;
        this.scheduler = createScheduler();
    }

    private ScheduledExecutorService createScheduler() {
        ThreadFactory threadFactory = r -> {
            Thread thread = new Thread(r);
            thread.setName("ticket-monitor-scheduler");
            thread.setDaemon(false);
            return thread;
        };
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    @PreDestroy
    public void shutdown() {
        log.info("正在关闭票务监控调度器...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("调度器未能正常关闭，强制终止");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("调度器关闭被中断，强制终止");
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("票务监控调度器已关闭");
    }

    public void startMonitor(List<LocalDate> targetDates) {
        stopMonitor();
        log.info("启动监控，目标日期: {}，间隔: {}秒", targetDates, properties.getMonitorInterval());
        currentTask = scheduler.scheduleWithFixedDelay(
                () -> executeMonitorCycle(targetDates),
                0,
                properties.getMonitorInterval(),
                TimeUnit.SECONDS
        );
    }

    public void stopMonitor() {
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel(false);
            log.info("已停止当前监控任务");
        }
    }

    void executeMonitorCycle(List<LocalDate> targetDates) {
        try {
            log.info("开始监控，目标日期: {}", targetDates);

            Map<String, List<ScheduleItem>> scheduleMap = apiClient.findSchedules(properties.getRouteId());
            if (log.isDebugEnabled()) {
                scheduleMap.values().stream().flatMap(List::stream).forEach(item ->
                        log.debug("车次 {} | 日期={} | 余票={}", item.getId(), item.getDate(), item.getNumber()));
            }

            // 取任意一个 scheduleId 用于查询路线名称
            Optional<ScheduleItem> anySchedule = scheduleMap.values().stream()
                    .flatMap(List::stream)
                    .findFirst();
            if (anySchedule.isEmpty()) {
                log.debug("无车次数据，本轮监控结束");
                return;
            }

            // 检查已支付订单，排除已购票日期
            Set<LocalDate> paidDates = orderService.findPaidOrderDates(
                    properties.getRouteId(), anySchedule.get().getId(), Set.copyOf(targetDates));
            List<LocalDate> remainingDates = targetDates.stream()
                    .filter(date -> !paidDates.contains(date))
                    .toList();
            if (remainingDates.isEmpty()) {
                log.debug("所有目标日期均已有已支付订单，本轮监控结束");
                return;
            }
            if (!paidDates.isEmpty()) {
                log.debug("排除已支付日期后，剩余监控日期: {}", remainingDates);
            }

            List<ScheduleItem> availableSchedules = filterAvailableSchedules(scheduleMap, remainingDates);
            log.debug("有票车次: {}个", availableSchedules.size());

            for (ScheduleItem schedule : availableSchedules) {
                processSchedule(schedule);
            }
            log.debug("本轮监控完成，等待{}秒后开始下一轮", properties.getMonitorInterval());
        } catch (UnpaidOrderException e) {
            // 发现待支付订单，写入日志并停止监控
            ticketLogService.writeUnpaidOrderWarning(e.getMessage());
            log.warn("发现待支付订单，监控已停止: {}", e.getMessage());
            stopMonitor();
        } catch (Exception e) {
            // ScheduledExecutor 最外层必须 catch，否则调度会永久停止
            log.error("本轮监控异常: {}", e.getMessage(), e);
        }
    }

    private List<ScheduleItem> filterAvailableSchedules(Map<String, List<ScheduleItem>> scheduleMap,
                                                         List<LocalDate> targetDates) {
        Set<LocalDate> targetDateSet = Set.copyOf(targetDates);
        return scheduleMap.values().stream()
                .flatMap(List::stream)
                .filter(item -> item.getNumber() > 0)
                .filter(item -> targetDateSet.contains(parseScheduleDate(item.getDate())))
                .toList();
    }

    private void processSchedule(ScheduleItem schedule) {
        try {
            log.debug("开始处理车次: id={}, 日期={}, 余票={}", schedule.getId(), schedule.getDate(), schedule.getNumber());
            boolean created = orderService.tryCreateOrder(schedule);
            if (created) {
                ticketLogService.logTicketPurchase(orderService.getLastCreatedOrderId());
            }
        } catch (UnpaidOrderException e) {
            // 待支付订单异常必须向上抛出，终止监控循环
            throw e;
        } catch (Exception e) {
            // 单个日期失败不中断其他日期的处理，下一轮会重新尝试
            log.error("处理车次 {} 异常: {}", schedule.getDate(), e.getMessage(), e);
        }
    }

    private LocalDate parseScheduleDate(String dateStr) {
        return LocalDate.parse(dateStr, SCHEDULE_DATE_FORMAT);
    }
}
