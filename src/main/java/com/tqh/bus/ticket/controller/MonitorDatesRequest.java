package com.tqh.bus.ticket.controller;

import java.util.List;

public class MonitorDatesRequest {

    private List<String> dates;

    public List<String> getDates() {
        return dates;
    }

    public void setDates(List<String> dates) {
        this.dates = dates;
    }
}
