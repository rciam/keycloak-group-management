package org.rciam.plugins.groups.helpers;

public class PagerParameters {

    private Integer first;
    private Integer max;
    private String order;
    private String orderType;

    public PagerParameters(Integer first, Integer max, String order, String orderType){
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

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }
}
