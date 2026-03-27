package com.tqh.bus.ticket.common;

public class ResultWrapper<T> {

    private int code;
    private String message;
    private T data;

    public ResultWrapper() {
    }

    public ResultWrapper(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResultWrapper<T> success(T data) {
        return new ResultWrapper<>(200, "操作成功", data);
    }

    public static ResultWrapper<Void> success() {
        return new ResultWrapper<>(200, "操作成功", null);
    }

    public static ResultWrapper<Void> fail(int code, String message) {
        return new ResultWrapper<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
