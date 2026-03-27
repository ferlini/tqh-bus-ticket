package com.tqh.bus.ticket.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItem {

    private int id;

    @JsonProperty("route_name")
    private String routeName;

    private OrderDescription description;

    @JsonProperty("trade_state")
    private String tradeState;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public OrderDescription getDescription() {
        return description;
    }

    public void setDescription(OrderDescription description) {
        this.description = description;
    }

    public String getTradeState() {
        return tradeState;
    }

    public void setTradeState(String tradeState) {
        this.tradeState = tradeState;
    }
}
