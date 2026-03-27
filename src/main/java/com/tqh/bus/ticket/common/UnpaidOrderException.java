package com.tqh.bus.ticket.common;

public class UnpaidOrderException extends RuntimeException {

    public UnpaidOrderException(String message) {
        super(message);
    }
}
