package com.tqh.bus.ticket.service;

import com.tqh.bus.ticket.common.BusinessException;
import com.tqh.bus.ticket.config.TqhProperties;
import com.tqh.bus.ticket.integration.TqhApiClient;
import com.tqh.bus.ticket.integration.model.OrderDescription;
import com.tqh.bus.ticket.integration.model.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TicketLogServiceTest {

    @Mock
    private TqhApiClient apiClient;

    @Mock
    private TqhProperties properties;

    @TempDir
    Path tempDir;

    private TicketLogService logService;
    private Path logFile;

    @BeforeEach
    void setUp() {
        logFile = tempDir.resolve("ticket.log");
        given(properties.getLogFilePath()).willReturn(logFile.toString());
        logService = new TicketLogService(apiClient, properties);
    }

    @Test
    void should_append_ticket_info_to_log_file() throws IOException {
        // given
        OrderItem order = createOrder(572468, "17号线-明珠线-上班",
                "长沙圩①", "科创中心西门", "2026-03-24 07:40:00");
        given(apiClient.getOrders("", 1, 10)).willReturn(List.of(order));

        // when
        logService.logTicketPurchase(572468);

        // then
        String content = Files.readString(logFile);
        assertThat(content).contains("日期: 2026-03-24 07:40:00");
        assertThat(content).contains("路线: 17号线-明珠线-上班");
        assertThat(content).contains("上车站: 长沙圩①");
        assertThat(content).contains("下车站: 科创中心西门");
    }

    @Test
    void should_format_log_with_separator() throws IOException {
        // given
        OrderItem order = createOrder(572468, "17号线-明珠线-上班",
                "长沙圩①", "科创中心西门", "2026-03-24 07:40:00");
        given(apiClient.getOrders("", 1, 10)).willReturn(List.of(order));

        // when
        logService.logTicketPurchase(572468);

        // then
        String content = Files.readString(logFile);
        assertThat(content).contains("----------------------------------------");
    }

    @Test
    void should_query_order_by_wx_order_id() throws IOException {
        // given
        OrderItem targetOrder = createOrder(572468, "17号线-明珠线-上班",
                "长沙圩①", "科创中心西门", "2026-03-24 07:40:00");
        OrderItem otherOrder = createOrder(571764, "4号线-上冲/香洲-下班",
                "厚朴道中(科创中心) ②", "蓝盾路口②", "2026-03-23 18:25:00");
        given(apiClient.getOrders("", 1, 10)).willReturn(List.of(targetOrder, otherOrder));

        // when
        logService.logTicketPurchase(572468);

        // then
        String content = Files.readString(logFile);
        assertThat(content).contains("路线: 17号线-明珠线-上班");
        assertThat(content).doesNotContain("4号线-上冲/香洲-下班");
    }

    @Test
    void should_throw_when_order_not_found() {
        // given
        given(apiClient.getOrders("", 1, 10)).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> logService.logTicketPurchase(999999))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("999999");
    }

    @Test
    void should_clear_log_file() throws IOException {
        // given
        Files.writeString(logFile, "some existing content");

        // when
        logService.clearLog();

        // then
        assertThat(Files.readString(logFile)).isEmpty();
    }

    @Test
    void should_not_throw_when_log_file_not_exists() {
        // given - logFile does not exist yet in tempDir

        // when & then - should not throw
        logService.clearLog();
    }

    private OrderItem createOrder(int id, String routeName, String startStop, String endStop, String dateTime) {
        OrderItem order = new OrderItem();
        order.setId(id);
        order.setRouteName(routeName);
        OrderDescription desc = new OrderDescription();
        desc.setStartStop(startStop);
        desc.setEndStop(endStop);
        desc.setDate(List.of(dateTime));
        order.setDescription(desc);
        return order;
    }
}
