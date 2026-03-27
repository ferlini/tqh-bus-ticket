package com.tqh.bus.ticket.integration.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScheduleFindRequest {

    @JsonProperty("route_id")
    private int routeId;

    public ScheduleFindRequest() {
    }

    public ScheduleFindRequest(int routeId) {
        this.routeId = routeId;
    }

    public int getRouteId() {
        return routeId;
    }

    public void setRouteId(int routeId) {
        this.routeId = routeId;
    }
}
