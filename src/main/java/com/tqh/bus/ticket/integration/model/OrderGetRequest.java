package com.tqh.bus.ticket.integration.model;

public class OrderGetRequest {

    private String status;
    private int page;
    private int limit;

    public OrderGetRequest() {
    }

    public OrderGetRequest(String status, int page, int limit) {
        this.status = status;
        this.page = page;
        this.limit = limit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
