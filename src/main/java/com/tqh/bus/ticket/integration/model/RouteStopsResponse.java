package com.tqh.bus.ticket.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RouteStopsResponse {

    @JsonProperty("route_id")
    private int routeId;

    @JsonProperty("route_name")
    private String routeName;

    private List<StopItem> up;
    private List<StopItem> down;

    public int getRouteId() {
        return routeId;
    }

    public void setRouteId(int routeId) {
        this.routeId = routeId;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public List<StopItem> getUp() {
        return up;
    }

    public void setUp(List<StopItem> up) {
        this.up = up;
    }

    public List<StopItem> getDown() {
        return down;
    }

    public void setDown(List<StopItem> down) {
        this.down = down;
    }
}
