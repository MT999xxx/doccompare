package com.example.doccompare.model;

import java.util.Calendar;
import java.util.Date;
import java.time.LocalDateTime;

public class User {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String phoneNumber;
    private String company;
    private Role role;
    private boolean enabled = true;
    private boolean active = true;
    private int credits = 0;
    private boolean vip = false;
    private int vipMonths = 0;
    private Date vipExpiry;
    private Date createdAt;
    private Date updatedAt;
    private LocalDateTime registrationTime;
    private LocalDateTime lastLoginTime;

    // 构造函数
    public User() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.registrationTime = LocalDateTime.now();
    }

    public User(Long id, String username, String password, String email, String phoneNumber, String company, Role role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.company = company;
        this.role = role;
        this.enabled = true;
        this.active = true;
        this.credits = role == Role.EMPLOYEE ? 0 : 5; // 员工无需积分，普通用户初始5积分
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.registrationTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public boolean isVip() {
        return vip || (vipExpiry != null && vipExpiry.after(new Date()));
    }

    public void setVip(boolean vip) {
        this.vip = vip;
    }

    public int getVipMonths() {
        return vipMonths;
    }

    public void setVipMonths(int months) {
        this.vipMonths = months;
        if (months > 0) {
            this.vip = true;
            // 设置VIP到期时间
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, months);
            this.vipExpiry = calendar.getTime();
        } else {
            this.vip = false;
            this.vipExpiry = null;
        }
    }

    public Date getVipExpiry() {
        return vipExpiry;
    }

    public void setVipExpiry(Date vipExpiry) {
        this.vipExpiry = vipExpiry;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getRegistrationTime() {
        return registrationTime;
    }

    public void setRegistrationTime(LocalDateTime registrationTime) {
        this.registrationTime = registrationTime;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public boolean isAdmin() {
        // 添加调试日志
        System.out.println("检查用户 " + username + " 是否为管理员, 角色: " + role);
        return role == Role.ADMIN || role == Role.SUPER_ADMIN;
    }

    public boolean isSuperAdmin() {
        return role == Role.SUPER_ADMIN;
    }

    public boolean isEmployee() {
        return role == Role.EMPLOYEE;
    }

    public void addCredits(int amount) {
        this.credits += amount;
        this.updatedAt = new Date();
    }

    public boolean deductCredits(int amount) {
        if (this.credits >= amount) {
            this.credits -= amount;
            this.updatedAt = new Date();
            return true;
        }
        return false;
    }
}