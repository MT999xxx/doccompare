package com.example.doccompare.model;

import java.util.Date;

public class Regulation {
    private Long id;
    private String title;
    private String content;
    private String category;
    private String createdBy;
    private String updatedBy;
    private Date createdAt;
    private Date updatedAt;
    private boolean enabled = true;

    // 构造函数
    public Regulation() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public Regulation(Long id, String title, String content, String category, String createdBy) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
        this.enabled = true;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "Regulation{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}