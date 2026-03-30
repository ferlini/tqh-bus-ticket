package com.tqh.bus.ticket.integration.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CouponRequest {

    @JsonProperty("route_id")
    private int routeId;

    @JsonProperty("schedule_ids")
    private List<Integer> scheduleIds;

    @JsonProperty("boarding_point_id")
    private int boardingPointId;

    public CouponRequest() {
    }

    public CouponRequest(int routeId, List<Integer> scheduleIds, int boardingPointId) {
        this.routeId = routeId;
        this.scheduleIds = scheduleIds;
        this.boardingPointId = boardingPointId;
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

    public int getBoardingPointId() {
        return boardingPointId;
    }

    public void setBoardingPointId(int boardingPointId) {
        this.boardingPointId = boardingPointId;
    }
}
