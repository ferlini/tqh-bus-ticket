package com.tqh.bus.ticket.integration.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RouteStopsRequest {

    @JsonProperty("route_id")
    private int routeId;

    @JsonProperty("schedule_ids")
    private List<Integer> scheduleIds;

    public RouteStopsRequest() {
    }

    public RouteStopsRequest(int routeId, List<Integer> scheduleIds) {
        this.routeId = routeId;
        this.scheduleIds = scheduleIds;
    }

    public int getRouteId() {
        return routeId;
    }

    public void setRouteId(int routeId) {
        this.routeId = routeId;
    }

    public List<Integer> getScheduleIds() {
        return scheduleIds;
    }

    public void setScheduleIds(List<Integer> scheduleIds) {
        this.scheduleIds = scheduleIds;
    }
}
