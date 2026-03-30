package com.tqh.bus.ticket.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultWrapperTest {

    @Test
    void should_create_success_result_with_data() {
        ResultWrapper<String> result = ResultWrapper.success("hello");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("操作成功");
        assertThat(result.getData()).isEqualTo("hello");
    }

    @Test
    void should_create_success_result_without_data() {
        ResultWrapper<Void> result = ResultWrapper.success();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("操作成功");
        assertThat(result.getData()).isNull();
    }

    @Test
    void should_create_fail_result_with_code_and_message() {
        ResultWrapper<Void> result = ResultWrapper.fail(400, "参数错误");

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("参数错误");
        assertThat(result.getData()).isNull();
    }
}
