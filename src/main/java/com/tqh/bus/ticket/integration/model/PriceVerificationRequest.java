package com.tqh.bus.ticket.integration.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class PriceVerificationRequest {

    @JsonProperty("route_id")
    private int routeId;

    @JsonProperty("schedule_ids")
    private List<Integer> scheduleIds;

    @JsonProperty("coupon_ids")
    private Map<String, Map<String, Integer>> couponIds;

    @JsonProperty("boarding_point_id")
    private int boardingPointId;

    @JsonProperty("alighting_point_id")
    private int alightingPointId;

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

    public Map<String, Map<String, Integer>> getCouponIds() {
        return couponIds;
    }

    public void setCouponIds(Map<String, Map<String, Integer>> couponIds) {
        this.couponIds = couponIds;
    }

    public int getBoardingPointId() {
        return boardingPointId;
    }

    public void setBoardingPointId(int boardingPointId) {
        this.boardingPointId = boardingPointId;
    }

    public int getAlightingPointId() {
        return alightingPointId;
    }

    public void setAlightingPointId(int alightingPointId) {
        this.alightingPointId = alightingPointId;
    }
}
