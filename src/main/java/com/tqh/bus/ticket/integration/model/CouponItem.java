package com.tqh.bus.ticket.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CouponItem {

    private int id;

    @JsonProperty("coupon_category_id")
    private int couponCategoryId;

    @JsonProperty("coupon_name")
    private String couponName;

    private double denomination;

    @JsonProperty("is_use")
    private Map<String, Boolean> isUse;

    private Map<String, String> status;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCouponCategoryId() {
        return couponCategoryId;
    }

    public void setCouponCategoryId(int couponCategoryId) {
        this.couponCategoryId = couponCategoryId;
    }

    public String getCouponName() {
        return couponName;
    }

    public void setCouponName(String couponName) {
        this.couponName = couponName;
    }

    public double getDenomination() {
        return denomination;
    }

    public void setDenomination(double denomination) {
        this.denomination = denomination;
    }

    public Map<String, Boolean> getIsUse() {
        return isUse;
    }

    public void setIsUse(Map<String, Boolean> isUse) {
        this.isUse = isUse;
    }

    public Map<String, String> getStatus() {
        return status;
    }

    public void setStatus(Map<String, String> status) {
        this.status = status;
    }
}
