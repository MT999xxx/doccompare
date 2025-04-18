// MembershipPlan.java
package com.example.doccompare.model;

public class MembershipPlan {
    private Long id;
    private String name;
    private double price;
    private int durationMonths;
    private int credits;
    private boolean active = true;

    // 构造函数
    public MembershipPlan() {}

    public MembershipPlan(Long id, String name, double price, int durationMonths, int credits) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.durationMonths = durationMonths;
        this.credits = credits;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(int durationMonths) {
        this.durationMonths = durationMonths;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}