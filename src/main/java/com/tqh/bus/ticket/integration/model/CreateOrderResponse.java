package com.tqh.bus.ticket.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateOrderResponse {

    @JsonProperty("wx_order_id")
    private int wxOrderId;

    @JsonProperty("is_zero_order")
    private boolean zeroOrder;

    public int getWxOrderId() {
        return wxOrderId;
    }

    public void setWxOrderId(int wxOrderId) {
        this.wxOrderId = wxOrderId;
    }

    public boolean isZeroOrder() {
        return zeroOrder;
    }

    public void setZeroOrder(boolean zeroOrder) {
        this.zeroOrder = zeroOrder;
    }
}
