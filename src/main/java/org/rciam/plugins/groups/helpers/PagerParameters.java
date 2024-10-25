package org.rciam.plugins.groups.helpers;

import java.util.List;

public class PagerParameters {

    private Integer first;
    private Integer max;
    private List<String> order;
    private String orderType;

    public PagerParameters(Integer first, Integer max, List<String> order, String orderType){
        this.first = first;
        this.max = max;
        this.order = order;
        this.orderType = orderType;
    }

    public Integer getFirst() {
        return first;
    }

    public void setFirst(Integer first) {
        this.first = first;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public List<String> getOrder() {
        return order;
    }

    public void setOrder(List<String> order) {
        this.order = order;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }
}
