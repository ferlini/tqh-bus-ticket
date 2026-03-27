package com.tqh.bus.ticket.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_return_business_error_when_business_exception_thrown() throws Exception {
        mockMvc.perform(post("/test/business-error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("余额不足"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void should_return_500_when_unexpected_exception_thrown() throws Exception {
        mockMvc.perform(post("/test/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("服务器内部错误"));
    }

    @RestController
    static class TestController {

        @PostMapping("/test/business-error")
        public ResultWrapper<Void> businessError() {
            throw new BusinessException(400, "余额不足");
        }

        @PostMapping("/test/unexpected-error")
        public ResultWrapper<Void> unexpectedError() {
            throw new RuntimeException("NPE");
        }
    }
}
